/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import java.nio.file.{Files, Path}

import org.romanowski.HoarderCommonSettings._
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

  private case class StashSetup(cacheSetup: CacheSetup, compilationResult: CompilationResult)

  private val doStashData = TaskKey[StashSetup]("doStashData", "Hoarder internals!")
  private val allToStash = TaskKey[Seq[StashSetup]]("allToStash", "Hoarder internals!")
  private val doStashApplyData = TaskKey[CacheSetup]("doStashApplyData", "Hoarder internals!")
  private val allToStashApply = TaskKey[Seq[CacheSetup]]("allToStashApply", "Hoarder internals!")
  private val globalCacheLocationScoped = InputKey[Path]("cacheLocationScoped", "Hoarder internals!")


  private val ImportConfig = config("cacheImport")
  private val ExportConfig = config("cacheExport")

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
    globalStashLocation := BuildPaths.getGlobalBase(state.value) / "sbt-stash",
    globalCacheLocationScoped.in(ImportConfig) := {
      val globalCache = askForStashLocation.evaluated
      assert(Files.isDirectory(globalCache) && Files.exists(globalCache),
        s"Cache does not exists in $globalCache (${new File(".").getAbsolutePath}!")
      globalCache
    },
    globalCacheLocationScoped.in(ExportConfig) := {
      val globalCache = askForStashLocation.evaluated
      assert(!Files.exists(globalCache) || overrideExistingCache.value, s"Cache already exists in $globalCache!")
      globalCache
    }
  )

  def settings = {
    def perConfigSettings = Seq(
      doStashData := StashSetup(projectSetupFor.value, compileIncremental.value),
      doStashApplyData := projectSetupFor.value
    )

    inConfig(Compile)(perConfigSettings) ++ inConfig(Test)(perConfigSettings) ++ Seq(
      allToStash := Seq(doStashData.in(Compile).value, doStashData.in(Test).value),
      stashKey := {
        val globalCache = globalCacheLocationScoped.in(ExportConfig).evaluated

        val exportedClasses = allToStash.value.map {
          case StashSetup(cache, result) =>
            exportCacheTaskImpl(cache, result, globalCache)
            cache.classesRoot
        }

        streams.value.log.info(s"Project ${name.value} stashed to $globalCache using classes from $exportedClasses")
      },
      allToStashApply := Seq(doStashApplyData.in(Compile).value, doStashApplyData.in(Test).value),
      stashApplyKey := {
        val globalCache = globalCacheLocationScoped.in(ImportConfig).evaluated

        val importedClasses = allToStashApply.value.map {
          cache =>
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


