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
    *    <do-hoarder-cache-export>
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

    /** `doExportCachePart` will export hoarder cache part to given path.
      * Please note that path is global cache so actual part will exported to subdirectory. */
    def exportCachePart(doExportCachePart: Path => Unit): Unit

    /** `doLoadCachePart` will load hoarder cache part from given path.
      * Please note that path is global cache so actual part will loaded form subdirectory. */
    def loadCachePart(doLoadCachePart: Path => Unit): Unit

    /** `doExportCache` will export cache for whole project to provided path */
    def exportCache(doExportCache: Path => Unit): Unit

    /** `doLoadCache` will load cache for whole project from provided path */
    def loadCache(doLoadCache: Path => Unit): Unit
  }

  private val doImportCiCaches = internalTask[Unit]("doImportCiCaches")
  private val doExportCiCaches = internalTask[Unit]("doExportCiCaches")


  def projectSettings = Seq(
    doImportCiCaches := cachedCiSetup.value.loadCachePart {
      path =>
        val paths = importCacheSetups.value.map {
          setup =>
            importCacheTaskImpl(setup, path)
            setup.relativeCacheLocation
        }
        streams.value.log.info(s"Cache imported from $paths")
    },
    doExportCiCaches := cachedCiSetup.value.exportCachePart {
      cachePath =>
        val paths = exportCacheSetups.value.map(exportCacheTaskImpl(cachePath)).map(_.toAbsolutePath)
        streams.value.log.info(s"Cache exported to $paths")
    },
    aggregate.in(doImportCiCaches) := true,
    aggregate.in(doExportCiCaches) := true,
    preBuild := Def.taskDyn {
      val setup = cachedCiSetup.value

      if (setup.shouldUseCache()) Def.task(setup.loadCache {
        path =>
          aggregatedTask(doImportCiCaches).value
          streams.value.log.info(s"Cache imported from $path")
      })
      else Def.task(streams.value.log.info(s"Cache won't be used."))
    }.value,
    postBuild := Def.taskDyn {
      val setup = cachedCiSetup.value
      if (setup.shouldPublishCaches()) {
        setup.invalidateCache()
        Def.task(cachedCiSetup.value.exportCache {
          path =>
            aggregatedTask(doExportCiCaches).value
            streams.value.log.info(s"Cache exported to $path")
        })
      } else Def.task(streams.value.log.info(s"Cache won't be published."))
    }.value,
    aggregate.in(preBuild) := false,
    aggregate.in(postBuild) := false
  )

  private def aggregatedTask(key: TaskKey[_]) = Def.task {
    state.map {
      state =>
        val extracted = Project.extract(state)
        extracted.runAggregated(key.in(extracted.currentRef), state)
    }.value
  }

  def globalSettings(setup: Setup) = Seq(
    cachedCiSetup := setup
  )

}