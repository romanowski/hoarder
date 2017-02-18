/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import sbt.Keys._
import sbt._

import org.romanowski.HoarderCommonSettings._

// TODO #8 Implement hoarder engine for 1.0
class HoarderEngine extends HoarderEngineCommon {

  type CompilationResult = xsbti.compile.CompileResult
  type PreviousCompilationResult = xsbti.compile.PreviousResult

  protected override def exportCacheTaskImpl(setup: CacheSetup,
                                             result: CompilationResult,
                                             globalCacheLocation: Path): Unit = {}

  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = None
}
