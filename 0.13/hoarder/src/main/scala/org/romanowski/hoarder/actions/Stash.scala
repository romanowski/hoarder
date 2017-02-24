/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import org.romanowski.HoarderCommonSettings._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Def._
import sbt.Keys._
import sbt._

object Stash extends HoarderEngine {

  val stashKey = InputKey[Unit]("stash", "Stash results of your current compilation")
  val stashApplyKey = InputKey[Unit]("stashApply", "Stash results of your current compilation")

  private case class StashSetup(cacheSetup: CacheSetup, compilationResult: CompilationResult)

  private val doStashData = TaskKey[StashSetup]("doStashData", "Stash results of your current compilation")
  private val allToStash = TaskKey[Seq[StashSetup]]("allToStash", "Stash results of your current compilation")

  private val doStashApplyData = TaskKey[CacheSetup]("doStashApplyData", "Stash results of your current compilation")
  private val allToStashApply = TaskKey[Seq[CacheSetup]]("allToStashApply", "Stash results of your current compilation")



  private def perConfigSettings = Seq(
    doStashData := StashSetup(projectSetupFor.value, compileIncremental.value),
    doStashApplyData := projectSetupFor.value
  )

  def settings =
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


