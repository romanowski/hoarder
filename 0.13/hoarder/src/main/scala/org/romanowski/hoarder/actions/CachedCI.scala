/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import java.nio.file.Path

import org.romanowski.HoarderPlugin
import org.romanowski.HoarderPlugin.autoImport._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Def._
import sbt.Keys._
import sbt._

object CachedCI extends HoarderEngine {

  abstract class PluginBase(setup: Setup) extends AutoPlugin {
    override def trigger: PluginTrigger = AllRequirements

    override def requires: Plugins = HoarderPlugin

    override def globalSettings: Seq[Def.Setting[_]] = CachedCI.globalSettings(setup)

    override def projectSettings: Seq[Def.Setting[_]] = CachedCI.projectSettings
  }

  trait Setup {
    def shouldPublishCaches(): Boolean

    def shouldUseCache(): Boolean

    def invalidateCache(): Unit

    def exportCachePart(op: Path => Unit): Unit

    def loadCachePart(op: Path => Unit): Unit

    def exportCache(op: Path => Unit): Unit

    def loadCache(op: Path => Unit): Unit
  }

  private val doImportCiCaches = HoarderPlugin.internalTask[Unit]("doImportCiCaches")
  private val doExportCiCaches = HoarderPlugin.internalTask[Unit]("doExportCiCaches")


  def projectSettings = Seq(
    doImportCiCaches := currentSetup.value.loadCachePart {
      path =>
        val paths = importCacheSetups.value.map { setup =>
          importCacheTaskImpl(setup, path)
          setup.relativeCacheLocation
        }
        streams.value.log.info(s"Cache imported from $paths")
    },
    doExportCiCaches := currentSetup.value.exportCachePart { cachePath =>
      val paths = exportCacheSetups.value.map(exportCacheTaskImpl(cachePath)).map(_.toAbsolutePath)
      streams.value.log.info(s"Cache exported to $paths")
    },
    aggregate.in(doImportCiCaches) := true,
    aggregate.in(doExportCiCaches) := true,
    preBuild := Def.taskDyn {
      val setup = currentSetup.value

      if (setup.shouldUseCache()) Def.task(setup.loadCache { path =>
        aggregatedTask(doImportCiCaches).value
        streams.value.log.info(s"Cache imported from $path")
      })
      else Def.task(streams.value.log.info(s"Cache won't be used."))
    }.value,
    postBuild := Def.taskDyn {
      val setup = currentSetup.value
      if (setup.shouldPublishCaches()) {
        setup.invalidateCache()
        Def.task(currentSetup.value.exportCache { path =>
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
    currentSetup := setup
  )

}