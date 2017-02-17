/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package pl.typosafe.demeter
import sbt._
import sbt.syntax._
import sbt.Keys._
import sbt.internal.inc.Analysis
import sbt.internal.inc.cached.CacheProvider
import sbt.util.InterfaceUtil._
import xsbti.Maybe
import xsbti.compile.PreviousResult

class DemeterPlugin {
  def settings: Seq[Setting[_]] = (compilationCacheProvider := Nil) +: Seq(Compile, Test).map(hookToScope)

  val compilationCacheProvider =
    TaskKey[Seq[CacheProvider]]("compilationCacheProvider")

  def hookToScope(s: Configuration): Setting[_] = compileInputs := {
    val original = compileInputs.value
    val previous = (m2o(original.previousResult().analysis()), m2o(original.previousResult().setup())) match {
      case (Some(analysis), Some(setup)) =>
        Some(analysis -> setup)
      case _ =>
        None
    }
    val newCache = compilationCacheProvider.value.toStream.flatMap(_.findCache(previous))
      .flatMap(_.loadCache(baseDirectory.value))
      .headOption

    newCache.fold(original) {
      case (analysis, setup) =>
        original.withPreviousResult(new PreviousResult(Maybe.just(analysis), Maybe.just(setup)))
    }
  }

}
