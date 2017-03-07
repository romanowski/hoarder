/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import sbt.compiler.{IC, MixedAnalyzingCompiler}
import sbt.inc.MappableFormat
import sbt.internal.inc.AnalysisMappers
import sbt.{PathFinder, _}
import xsbti.compile.SingleOutput


class HoarderEngine extends HoarderEngineCommon {

  type CompilationResult = IC.Result
  type PreviousCompilationResult = Compiler.PreviousAnalysis


  protected override def exportCacheTaskImpl(cacheSetup: CacheSetup,
                                             result: CompilationResult,
                                             globalCacheLocation: Path): Unit = {
    import cacheSetup._
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)

    if (Files.exists(cacheLocation)) {
      if(overrideExistingCache) IO.delete(cacheLocation.toFile)
      else new IllegalArgumentException(s"Cache already exists at $cacheLocation.")
    }

    Files.createDirectories(cacheLocation)

    val mapper = createMapper(cacheSetup)
    val fos = Files.newBufferedWriter(cacheLocation.resolve(analysisCacheFileName), Charset.forName("UTF-8"))
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
    } finally fos.close()

    val outputPath = ouputForProject(result.setup).toPath

    val classes = (PathFinder(classesRoot.toFile) ** "*.class").get
    val classesToZip = classes.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    IO.zip(classesToZip, cacheLocation.resolve(classesZipFileName).toFile)
  }

  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = {
    import cacheSetup._
    val cacheLocation = cacheSetup.cacheLocation(globalCacheLocation)

    val from = cacheLocation.resolve(analysisCacheFileName)
    val classesZip = cacheLocation.resolve(classesZipFileName)
    val mapper = createMapper(cacheSetup)
    val outputDir = classesRoot.toFile

    if (Files.exists(from) && Files.exists(classesZip)) {

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

      val ios = Files.newBufferedReader(from, Charset.forName("UTF-8"))

      val (analysis, setup) = try {
        new MappableFormat(mapper).read(ios)
      } finally ios.close()

      val store = MixedAnalyzingCompiler.staticCachedStore(analysisFile)
      store.set(analysis, setup)

      Some(Compiler.PreviousAnalysis(analysis, Some(setup)))
    } else None
  }

  private def createMapper(projectSetup: CacheSetup): AnalysisMappers = {
    import projectSetup._
    new SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath)
  }

  private def ouputForProject(setup: CompileSetup): File = setup.output match {
    case s: SingleOutput =>
      s.outputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }
}
