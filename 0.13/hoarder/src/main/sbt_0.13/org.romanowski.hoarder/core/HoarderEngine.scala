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

import org.romanowski.HoarderPlugin.CacheSetup
import org.romanowski.hoarder.core.SbtTypes.CompilationResult
import org.romanowski.hoarder.core.SbtTypes.PreviousCompilationResult
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

  protected override def exportCacheTaskImpl(cacheSetup: CacheSetup,
                                             result: CompilationResult,
                                             globalCacheLocation: Path): Path = {
    import cacheSetup._
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)
    assert(!Files.exists(cacheLocation) || overrideExistingCache, s"Cache already exists in $cacheLocation!")

    if (Files.exists(cacheLocation)) {
      if (overrideExistingCache) IO.delete(cacheLocation.toFile)
      else new IllegalArgumentException(s"Cache already exists at $cacheLocation.")
    }

    Files.createDirectories(cacheLocation)

    val mapper = createMapper(cacheSetup)

    val exportedAnalysisPath = if (cacheSetup.zipAnalysisFile)
      analysisPathFromZip(cacheLocation.resolve(analysisCacheZipFileName), create = true)
    else
      cacheLocation.resolve(analysisCacheFileName)

    val fos = Files.newBufferedWriter(exportedAnalysisPath, charset)
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
    } finally {
      fos.close()
      if (cacheSetup.zipAnalysisFile) exportedAnalysisPath.getFileSystem.close()
    }

    val outputPath = outputForProject(result.setup).toPath

    val classes = (PathFinder(classesRoot.toFile) ** "*.class").get
    val classesToZip = classes.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    IO.zip(classesToZip, cacheLocation.resolve(classesZipFileName).toFile)
    cacheLocation
  }

  private def zipAnalysis(cacheLocation: Path): Option[Path] = {
    val zipPath = cacheLocation.resolve(analysisCacheZipFileName)
    if (Files.exists(zipPath)) Option(analysisPathFromZip(zipPath)) else None
  }

  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = {
    import cacheSetup._
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)
    assert(Files.isDirectory(cacheLocation) && Files.exists(cacheLocation), s"Cache does not exists in $cacheLocation")

    val mapper = createMapper(cacheSetup)
    val outputDir = classesRoot.toFile


    val zippedAnalysis = zipAnalysis(cacheLocation)
    try {
      val analysisPath = zippedAnalysis getOrElse cacheLocation.resolve(analysisCacheFileName)
      val classesZip = cacheLocation.resolve(classesZipFileName)
      if (Files.exists(classesZip) && Files.exists(analysisPath)) {
        if (outputDir.exists()) {
          if (outputDir.isDirectory) {
            cleanOutputMode match {
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

        IO.unzip(classesZip.toFile, outputDir, preserveLastModified = true)

        val analysisReader = Files.newBufferedReader(analysisPath, charset)
        val (analysis, setup) = try {
          new MappableFormat(mapper).read(analysisReader)
        } finally analysisReader.close()

        val store = MixedAnalyzingCompiler.staticCachedStore(analysisFile)
        store.set(analysis, setup)

        Option(Compiler.PreviousAnalysis(analysis, Some(setup)))
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
