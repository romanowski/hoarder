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
import sbt.Compiler.PreviousAnalysis
import sbt.PathFinder
import sbt._
import sbt.compiler.MixedAnalyzingCompiler
import sbt.inc.MappableFormat
import sbt.internal.inc.AnalysisMappers
import xsbti.compile.SingleOutput


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

    val exportedAnalysisPath =
      if (cacheSetup.zipAnalysisFile) cacheLocation.resolve(analysisCacheZipFileName)
      else cacheLocation.resolve(analysisCacheFileName)

    val writablePath =
      if (cacheSetup.zipAnalysisFile) analysisPathFromZip(exportedAnalysisPath, create = true)
      else exportedAnalysisPath

    val fos = Files.newBufferedWriter(writablePath, charset)
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
      exportedAnalysisPath
    } finally {
      fos.close()
      if (cacheSetup.zipAnalysisFile) writablePath.getFileSystem.close()
    }
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

  private def zipAnalysis(cacheLocation: Path): Option[Path] = {
    val zipPath = cacheLocation.resolve(analysisCacheZipFileName)
    if (Files.exists(zipPath)) Option(analysisPathFromZip(zipPath)) else None
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

  protected def importAnalysis(cacheSetup: CacheSetup, analysisPath: Path): PreviousAnalysis = {
    val mapper = createMapper(cacheSetup)

    val analysisReader = Files.newBufferedReader(analysisPath, charset)
    val (analysis, setup) = try {
      new MappableFormat(mapper).read(analysisReader)
    } finally analysisReader.close()

    val store = MixedAnalyzingCompiler.staticCachedStore(cacheSetup.analysisFile)
    store.set(analysis, setup)

    Compiler.PreviousAnalysis(analysis, Some(setup))
  }

  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = {
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)
    assert(
      Files.isDirectory(cacheLocation) && Files.exists(cacheLocation),
      s"Cache does not exists in ${cacheLocation.toAbsolutePath()}")

    val zippedAnalysis = zipAnalysis(cacheLocation)
    try {
      val analysisPath = zippedAnalysis getOrElse cacheLocation.resolve(analysisCacheFileName)
      val classesZip = cacheLocation.resolve(classesZipFileName)

      if (Files.exists(classesZip) && Files.exists(analysisPath)) {
        prepareImportDirectory(cacheSetup)
        importBinaries(cacheSetup, classesZip)

        Option(importAnalysis(cacheSetup, analysisPath))
      } else None
    } finally zippedAnalysis.foreach(_.getFileSystem.close())
  }

  private def createMapper(projectSetup: CacheSetup): AnalysisMappers = {
    import projectSetup._
    new SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath)
  }

  private def outputForProject(setup: CompileSetup): File = setup.output match {
    case s: SingleOutput =>
      s.outputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }
}
