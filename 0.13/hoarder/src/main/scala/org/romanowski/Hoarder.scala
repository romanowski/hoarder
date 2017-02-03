package org.romanowski

import java.nio.file.Path

import sbt.Keys._
import sbt._
import sbt.testing.TaskDef

object Hoarder extends HoarderEngine {
  val exportCacheLocation = SettingKey[Path]("exportCacheLocation", "Where cache should be exported")
  val exportCache = TaskKey[Path]("exportCache", "Export compilation cache to predefined location")
  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")

  val importCache = TaskKey[Unit]("importCache")

  private val doExportCache = TaskKey[Path]("doExportCache", "Exports cache for given scope")

  def doExportCacheImpl = Def.task {
    val location = exportCacheLocation.value
    val result = compileIncremental.value

    exportCacheTaskImpl(projectSetupFor.value, location, result)
    streams.value.log.info(s"Cache exported tp $location")

    location
  }

  def importCacheImpl = Def.task {
    val location = exportCacheLocation.value
    streams.value.log.info(s"Importing cache from: $location")

    val res = importCacheTaskImpl(projectSetupFor.value, cleanOutputMode.value, location)
      .getOrElse(previousCompile.value)

    streams.value.log.info(s"Done")
    res
  }

  def useStaticCache = Seq(
    // exportCacheLocation := target.value.toPath.getParent.resolve("cache"),
    cleanOutputMode := CleanClasses
  ) ++ inConfig(Compile)(Seq(previousCompile <<= importCacheImpl, doExportCache <<= doExportCacheImpl)) ++
    inConfig(Compile)(Seq(previousCompile <<= importCacheImpl, doExportCache <<= doExportCacheImpl)) ++ Seq(
    exportCache := {
      (doExportCache in Compile).value
      (doExportCache in Test).value

    }
  )

  def useStash = HoarderStash.settings

}