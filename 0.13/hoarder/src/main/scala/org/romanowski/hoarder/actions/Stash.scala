/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import java.nio.file.{Files, Path}

import org.romanowski.HoarderSettings._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Def._
import sbt.Keys._
import sbt._

object Stash extends HoarderEngine {

  val stashKey = InputKey[Unit]("stash",
    "Export results of current compilation to global location.")
  val stashApplyKey = InputKey[Unit]("stashApply",
    "Load exported copilation results from global location.")
  val defaultVersionLabel = SettingKey[String]("defaultVersionLabel",
    "Default version label for stash/stashApply")
  val defaultProjectLabel = SettingKey[String]("defaultProjectLabel",
    "Default root project label for stash/stashApply")
  val globalStashLocation = TaskKey[File]("globalStashLocation",
    "Root directory where exported artifacts are kept.")

  val parser = {
    import sbt.complete.Parser._
    import sbt.complete.Parsers._

    Space.* ~> (Space ~> token(StringBasic, "<project-label>")).?
      .flatMap { res =>
        (Space.+ ~> token(StringBasic, "<version-label>")).?.map(res -> _)
      } <~ Space.*
  }


  private def askForStashLocation = Def.inputTask {
    val (providedLabel, providedVersion) = parser.parsed

    val currentGlobalLabel = providedLabel.getOrElse(defaultProjectLabel.value)
    val currentLocalLabel = providedVersion.getOrElse(defaultVersionLabel.value)

    val file = globalStashLocation.value / currentGlobalLabel / currentLocalLabel
    file.toPath
  }

  def globalSettings = Seq(
    defaultVersionLabel := "HEAD",
    defaultProjectLabel := file(".").getAbsoluteFile.getParentFile.getName,
    globalStashLocation := BuildPaths.getGlobalBase(state.value) / "sbt-stash"
  )

  def settings = {
    Seq(
      stashKey := {
        val globalCache = askForStashLocation.evaluated.resolve(scalaBinaryVersion.value)

        val exportedClasses = exportCacheSetups.value.map {
          case ExportCacheSetup(cache, result) =>
            val targetCache = cache.cacheLocation(globalCache)
            assert(!Files.exists(targetCache) || overrideExistingCache.value, s"Cache already exists in $targetCache!")

            exportCacheTaskImpl(cache, result, globalCache)
            cache.classesRoot
        }

        streams.value.log.info(s"Project ${name.value} stashed to $globalCache using classes from $exportedClasses")
      },
      stashApplyKey := {
        val globalCache = askForStashLocation.evaluated.resolve(scalaBinaryVersion.value)

        val importedClasses = importCacheSetups.value.map {
          cache =>
            val targetCache = cache.cacheLocation(globalCache)
            assert(Files.isDirectory(targetCache) && Files.exists(targetCache),
              s"Cache does not exists in $targetCache")

            importCacheTaskImpl(cache, globalCache)
            cache.classesRoot
        }

        streams.value.log.info(s"Stash for ${name.value} applied from $globalCache to $importedClasses")
      },
      aggregate.in(stashKey) := true,
      aggregate.in(stashApplyKey) := true
    )
  }
}


