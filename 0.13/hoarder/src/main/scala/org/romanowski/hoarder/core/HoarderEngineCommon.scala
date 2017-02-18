/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.nio.charset.Charset

import sbt._
import sbt.Keys._
import java.nio.file.{Path, Paths}

import org.romanowski.HoarderCommonSettings._

trait HoarderEngineCommon {
  type CompilationResult
  type PreviousCompilationResult

  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  case class CacheSetup(sourceRoots: Seq[File],
                        classpath: Classpath,
                        classesRoot: Path,
                        projectRoot: Path,
                        analysisFile: File,
                        relativeCacheLocation: Path,
                        overrideExistingCache: Boolean,
                        cleanOutputMode: CleanOutputMode
                       )

  protected def exportCacheTaskImpl(setup: CacheSetup, result: CompilationResult, globalCacheLocation: Path): Unit

  protected def importCacheTaskImpl(cacheSetup: CacheSetup, globalCacheLocation: Path): Option[PreviousCompilationResult]

  protected def projectSetupFor = Def.task[CacheSetup] {
      CacheSetup(
        sourceRoots = managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
        classpath = externalDependencyClasspath.value,
        classesRoot = classDirectory.value.toPath,
        projectRoot = baseDirectory.value.toPath,
        analysisFile = (streams in compileIncSetup).value.cacheDirectory / compileAnalysisFilename.value,
        relativeCacheLocation = Paths.get(name.value).resolve(configuration.value.name),
        overrideExistingCache = overrideExistingCache.value,
        cleanOutputMode = cleanOutputMode.value
      )
    }
}