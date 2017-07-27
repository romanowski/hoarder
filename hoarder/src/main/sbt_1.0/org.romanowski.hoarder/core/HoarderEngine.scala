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

  protected override def exportCacheTaskImpl(cacheSetup: CacheSetup,
                                             result: CompilationResult,
                                             globalCacheLocation: Path): ExportedCache =
    compilationCache(cacheSetup, globalCacheLocation).exportCache(result.asAnalysisContents)


  protected override def importCacheTaskImpl(cacheSetup: CacheSetup,
                                             globalCacheLocation: Path): Option[PreviousCompilationResult] = {

    compilationCache(cacheSetup, globalCacheLocation).loadCache().map { content =>
      val importedStore = MixedAnalyzingCompiler.staticCachedStore(cacheSetup.analysisFile, useTextAnalysis = false)
      importedStore.set(content)

      content.asPreviousResults
    }
  }

  protected def compilationCache(cacheSetup: CacheSetup, globalCacheLocation: Path): CompilationCache =
    new HoarderCompilationCache(cacheSetup, globalCacheLocation)
}
