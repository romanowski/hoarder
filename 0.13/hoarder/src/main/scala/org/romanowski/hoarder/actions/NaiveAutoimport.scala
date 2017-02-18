/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import java.nio.file.Path

import org.romanowski.HoarderCommonSettings._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Keys._
import sbt._

class NaiveAutoimport extends HoarderEngine {
  val exportCache = TaskKey[Path]("exportCache", "Export compilation cache to predefined location")
  val importCache = TaskKey[Unit]("importCache")

  private val doExportCache = TaskKey[Path]("doExportCache", "Exports cache for given scope")


  private def doExportCacheImpl = Def.task {
    val location = staticCacheLocation.value
    val result = compileIncremental.value

    exportCacheTaskImpl(projectSetupFor.value(location), result)
    streams.value.log.info(s"Cache exported tp $location")

    location
  }

  def importCacheImpl = Def.task {
    val location = staticCacheLocation.value
    streams.value.log.info(s"Importing cache from: $location")

    val res = importCacheTaskImpl(projectSetupFor.value(location))
      .getOrElse(previousCompile.value)

    streams.value.log.info(s"Done")
    res
  }

  def settings =
    inConfig(Compile)(Seq(previousCompile := importCacheImpl.value, doExportCache := doExportCacheImpl.value)) ++
    inConfig(Compile)(Seq(previousCompile := importCacheImpl.value, doExportCache := doExportCacheImpl.value)) ++ Seq(
    exportCache := {
      (doExportCache in Compile).value
      (doExportCache in Test).value
    }
  )
}
