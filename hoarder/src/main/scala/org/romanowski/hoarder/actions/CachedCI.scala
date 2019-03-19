/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski
package hoarder.actions

import java.nio.file.Path

import org.romanowski.HoarderKeys._
import org.romanowski.hoarder.actions.Stash.stashParser
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
    def invalidateCache(prefix: String): Unit

    /** `doExportCache` will export cache for whole project to provided path */
    def exportCache(prefix: String): CacheProgress

    /** `doLoadCache` will load cache for whole project from provided path */
    def loadCache(prefix: String): CacheProgress

    def prefixFor(providedProject: Option[String], providedVersion: Option[String]): String =
      providedProject.getOrElse("default") + "-" + providedVersion.getOrElse("version")

    private[romanowski] final def prefix(input: (Option[String], Option[String])) = prefixFor(input._1, input._2)

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
    preBuild := loadCache.toTask("").value,
    postBuild := storeCache.toTask("").value,
    aggregate.in(preBuild) := false,
    aggregate.in(postBuild) := false,
    aggregate.in(storeCache) := false,
    aggregate.in(loadCache) := false,
    loadCache := Def.inputTaskDyn {
      val setup = cachedCiSetup.value

      val prefix = setup.prefix(stashParser.parsed)
      if (setup.shouldUseCache()) invokeOnProgress(setup.loadCache(prefix), doImportCiCaches)
      else Def.task(streams.value.log.info(s"Cache won't be used."))
    }.evaluated,
    storeCache := Def.inputTaskDyn {
      val setup = cachedCiSetup.value
      val prefix = setup.prefix(stashParser.parsed)
      if (setup.shouldPublishCaches()) {
        setup.invalidateCache(prefix)
        invokeOnProgress(setup.exportCache(prefix), doExportCiCaches)
      } else Def.task(streams.value.log.info(s"Cache won't be published."))
    }.evaluated,
    cleanCiCaches := {
      val setup = cachedCiSetup.value
      setup.invalidateCache(setup.prefix(stashParser.parsed))
    }
  )


  private def aggregatedTaskOnState(key: TaskKey[_])(state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.runAggregated(key.in(extracted.currentRef), state)
  }

  def globalSettings(setup: Setup) = Seq(
    cachedCiSetup := setup
  )

}