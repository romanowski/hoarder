/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util

import org.romanowski.HoarderKeys.CacheSetup
import org.romanowski.HoarderKeys.ExportedCache
import org.romanowski.hoarder.core.SbtTypes.CompilationResult
import org.romanowski.hoarder.core.SbtTypes.PreviousCompilationResult
import sbt.PathFinder
import sbt._
import xsbti.compile._
import SbtTypes._
import sbt.internal.inc.MixedAnalyzingCompiler
import sbt.internal.inc.FileAnalysisStore
import xsbti.compile.PreviousResult
import java.util.Optional
import xsbti.compile.AnalysisStore
import xsbti.compile.analysis._

class HoarderEngine extends HoarderEngineCommon {

  val charset = Charset.forName("UTF-8")

  private def analysisPathFromZip(zipPath: Path, create: Boolean = false) = {
    val uri = URI.create(s"jar:file:${zipPath.toAbsolutePath}")
    val env = new util.HashMap[String, String]()

    if (create) env.put("create", "true")
    FileSystems
      .newFileSystem(uri, env, null)
      .getPath("/", analysisCacheFileName)
  }

  protected def cleanupAndPrepareCacheLocation(cacheLocation: Path, cacheSetup: CacheSetup): Unit = {
    assert(!Files.exists(cacheLocation) || cacheSetup.overrideExistingCache,
      s"Cache already exists in $cacheLocation!")

    if (Files.exists(cacheLocation)) {
      if (cacheSetup.overrideExistingCache) IO.delete(cacheLocation.toFile)
      else new IllegalArgumentException(s"Cache already exists at $cacheLocation.")
    }

    Files.createDirectories(cacheLocation)
  }

  protected def exportAnalysis(cacheLocation: Path, cacheSetup: CacheSetup, result: CompilationResult): Path = {
    val mapper = createMapper(cacheSetup)

    val exportedAnalysisPath = cacheLocation.resolve(analysisCacheZipFileName)

    val exportedStore = FileAnalysisStore.binary(exportedAnalysisPath.toFile, mapper)

    exportedStore.set(result.asAnalysisContents)

    cacheLocation
  }

  protected def exportBinaries(cacheLocation: Path, cacheSetup: CacheSetup, result: CompilationResult): Option[Path] = {
    val outputPath = outputForProject(result.setup).toPath

    val classes = (PathFinder(cacheSetup.classesRoot.toFile) ** "*.class").get
    val classesToZip = classes.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    val zippedBinaries = cacheLocation.resolve(classesZipFileName)
    IO.zip(classesToZip, zippedBinaries.toFile)
    Option(zippedBinaries)
  }

  protected override def exportCacheTaskImpl(cacheSetup: CacheSetup,
                                             result: CompilationResult,
                                             globalCacheLocation: Path): ExportedCache = {
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)

    cleanupAndPrepareCacheLocation(cacheLocation, cacheSetup)



    ExportedCache(exportAnalysis(cacheLocation, cacheSetup, result), exportBinaries(cacheLocation, cacheSetup, result))
  }


  protected def prepareImportDirectory(cacheSetup: CacheSetup): Unit = {
    val outputDir = cacheSetup.classesRoot.toFile
    if (outputDir.exists()) {
      if (outputDir.isDirectory) {
        cacheSetup.cleanOutputMode match {
          case CleanOutput =>
            if (outputDir.list().nonEmpty) IO.delete(outputDir)
          case FailOnNonEmpty =>
            if (outputDir.list().nonEmpty)
              throw new IllegalStateException(s"Output directory: $outputDir is not empty and cleanOutput is false")
          case CleanClasses =>
            val classFiles = PathFinder(outputDir) ** "*.class"
            IO.delete(classFiles.get)
        }
      } else throw new IllegalStateException(s"Output file: $outputDir is not a directory")
    }
  }

  protected def importBinaries(cacheSetup: CacheSetup, classesZip: Path): Unit = {
    IO.unzip(classesZip.toFile, cacheSetup.classesRoot.toFile, preserveLastModified = true)
  }

  protected def importAnalysis(cacheSetup: CacheSetup, analysisPath: Path): PreviousResult = {
    val mapper = createMapper(cacheSetup)

    val exportedStore = FileAnalysisStore.binary(analysisPath.toFile, mapper)
    val content = exportedStore.get().get() //TODO do something more clever here!


    val importedStore = MixedAnalyzingCompiler.staticCachedStore(cacheSetup.analysisFile, useTextAnalysis = false)
    importedStore.set(content)

    PreviousResult.of(Optional.of(content.getAnalysis()), Optional.of(content.getMiniSetup()))
  }

  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = {
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)
    assert(
      Files.isDirectory(cacheLocation) && Files.exists(cacheLocation),
      s"Cache does not exists in ${cacheLocation.toAbsolutePath()}")

    val analysisPath = cacheLocation.resolve(analysisCacheZipFileName)
    val classesZip = cacheLocation.resolve(classesZipFileName)

    if (Files.exists(classesZip) && Files.exists(analysisPath)) {
      prepareImportDirectory(cacheSetup)
      importBinaries(cacheSetup, classesZip)

      Option(importAnalysis(cacheSetup, analysisPath))
    } else None

  }

  private def createMapper(projectSetup: CacheSetup): ReadWriteMappers = {
    import projectSetup._
    SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath).mappers
  }

  private def outputForProject(setup: MiniSetup): File = setup.output match {
    case s: SingleOutput =>
      s.getOutputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }
}
