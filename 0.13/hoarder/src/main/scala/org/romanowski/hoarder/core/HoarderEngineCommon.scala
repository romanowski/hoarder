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
import SbtTypes.CompilationResult
import SbtTypes.PreviousCompilationResult


import org.romanowski.HoarderSettings._

trait HoarderEngineCommon {
  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  protected def exportCacheTaskImpl(setup: CacheSetup, result: CompilationResult, globalCacheLocation: Path): Unit

  protected def importCacheTaskImpl(cacheSetup: CacheSetup, globalCacheLocation: Path): Option[PreviousCompilationResult]
}