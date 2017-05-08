/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.nio.file.Path

import org.romanowski.HoarderSettings._
import org.romanowski.hoarder.core.SbtTypes.CompilationResult
import org.romanowski.hoarder.core.SbtTypes.PreviousCompilationResult

trait HoarderEngineCommon {
  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  protected def exportCacheTaskImpl(setup: CacheSetup, result: CompilationResult, globalCacheLocation: Path): Path

  protected final def exportCacheTaskImpl(globalCacheLocation: Path)(setup: ExportCacheSetup): Path =
    exportCacheTaskImpl(setup.cacheSetup, setup.compilationResult, globalCacheLocation)


  protected def importCacheTaskImpl(cacheSetup: CacheSetup, globalCacheLocation: Path): Option[PreviousCompilationResult]
}