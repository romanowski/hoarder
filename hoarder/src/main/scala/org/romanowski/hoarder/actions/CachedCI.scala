/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski
package hoarder.actions

import java.nio.file.Path

import org.romanowski.HoarderKeys._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Keys._
import sbt._

object CachedCI extends HoarderEngine {

  abstract class PluginBase(setup: Setup) extends AutoPlugin {
    override def trigger: PluginTrigger = AllRequirements

    override def requires: Plugins = HoarderPlugin

    override def globalSettings: Seq[Def.Setting[_]] = CachedCI.globalSettings(setup)

    override def projectSettings: Seq[Def.Setting[_]] = CachedCI.projectSettings
  }

  /** This trait describes your CI flow.
    *
    * `preBuild` sbt task will invoke:
    * if (setup.shouldUseCache()) <do-hoarder-cache-import>
    *
    * `postBuild` sbt task will invoke:
    * if (setup.shouldPublishCaches()){
    *    setup.invalidateCache()
    * <do-hoarder-cache-export>
    * }
    *
    * Cache export/import (whole process) will be invoked inside exportCache/loadCache methods
    * Each individual cache entry (e.g. for compile in project foo) will be exported/imported inside exportCachePart/loadCachePart methods.
    */
  trait Setup {

    /** Should cache be publish for this build. Used inside postBuild task. */
    def shouldPublishCaches(): Boolean

    /** Should cache be used for this build. Used inside preBuild task */
    def shouldUseCache(): Boolean

    /** Remove current cache entry for this job. Called before new cache is exported */
    def invalidateCache(): Unit

    /** `doExportCache` will export cache for whole project to provided path */
    def exportCache(): CacheProgress

    /** `doLoadCache` will load cache for whole project from provided path */
    def loadCache(): CacheProgress
  }

  trait CacheProgress {
    def nextPart[T](op: Path => T): T
    def done(): Unit
  }

  private val doImportCiCaches = internalTask[Unit]("doImportCiCaches")
  private val doExportCiCaches = internalTask[Unit]("doExportCiCaches")


  private val currentState = AttributeKey[CacheProgress]("current-export-cache-progress")

  private def importPartFromProgress(progress: CacheProgress)(setup: CacheSetup): Path = {
    progress.nextPart(importCacheTaskImpl(setup, _))
    setup.relativeCacheLocation
  }

  private def exportPartFromProgress(progress: CacheProgress)(setup: ExportCacheSetup): Path =
    progress.nextPart(exportCacheTaskImpl(_)(setup)).analysis.toAbsolutePath

  private def invokeOnProgress(progress: CacheProgress, task: TaskKey[_]) = state.map {
    state =>
      val newState = state.update(currentState)(_ => progress)
      aggregatedTaskOnState(task)(newState)
      state.remove(currentState)
      progress.done()
      ()
  }

  def projectSettings = Seq(
    doImportCiCaches := {
      val progress = state.value.attributes(currentState)
      val setups = importCacheSetups.value
      val paths = setups.map(importPartFromProgress(progress))
      streams.value.log.info(s"Cache imported from $paths")
    },
    doExportCiCaches := {
      val progress = state.value.attributes(currentState)
      val setups = exportCacheSetups.value
      val paths = setups.map(exportPartFromProgress(progress))
      streams.value.log.info(s"Cache exported to $paths")
    },
    aggregate.in(doImportCiCaches) := true,
    aggregate.in(doExportCiCaches) := true,
    preBuild := Def.taskDyn {
      val setup = cachedCiSetup.value

      if (setup.shouldUseCache()) invokeOnProgress(setup.loadCache(), doImportCiCaches)
      else Def.task(streams.value.log.info(s"Cache won't be used."))
    }.value,
    postBuild := Def.taskDyn {
      val setup = cachedCiSetup.value
      if (setup.shouldPublishCaches()) {
        setup.invalidateCache()
        invokeOnProgress(setup.exportCache(), doExportCiCaches)
      } else Def.task(streams.value.log.info(s"Cache won't be published."))
    }.value,
    aggregate.in(preBuild) := false,
    aggregate.in(postBuild) := false,
    cleanCiCaches := cachedCiSetup.value.invalidateCache()
  )

  private def aggregatedTaskOnState(key: TaskKey[_])(state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.runAggregated(key.in(extracted.currentRef), state)
  }

  def globalSettings(setup: Setup) = Seq(
    cachedCiSetup := setup
  )

}