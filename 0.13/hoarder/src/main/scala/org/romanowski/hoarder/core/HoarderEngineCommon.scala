/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.nio.charset.Charset

import sbt._
import sbt.Keys._
import java.nio.file.Path

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
                        cacheLocation: Path,
                        overrideExistingCache: Boolean,
                        cleanOutputMode: CleanOutputMode
                       )

  protected def exportCacheTaskImpl(setup: CacheSetup, result: CompilationResult): Unit

  protected def importCacheTaskImpl(cacheSetup: CacheSetup): Option[PreviousCompilationResult]

  protected def projectSetupFor = Def.task[Path => CacheSetup] {
    path => {
      CacheSetup(
        sourceRoots = managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
        classpath = externalDependencyClasspath.value,
        classesRoot = classDirectory.value.toPath,
        projectRoot = baseDirectory.value.toPath,
        analysisFile = (streams in compileIncSetup).value.cacheDirectory / compileAnalysisFilename.value,
        cacheLocation = path.resolve(name.value).resolve(configuration.value.name),
        overrideExistingCache = overrideExistingCache.value,
        cleanOutputMode = cleanOutputMode.value
      )
    }
  }

}