package org.romanowski

import sbt._
import sbt.Def._
import sbt.Keys._


object HoarderStash extends HoarderEngine {

  val parser = spaceDelimited("<export-label>")

  val stashKey = TaskKey[Unit]("stash", "Stash results of your current compilation")
  val stashApplyKey = TaskKey[Unit]("stashApply", "Stash results of your current compilation")

  private val doStashKey = InputKey[Unit]("doStashKey", "Stash results of your current compilation")
  private val doStashApplyKey = InputKey[Unit]("doStashApplyKey", "Apply stashed results of your current compilation")


  val defaultVersionLabel = SettingKey[String]("Default label for project")
  val globalLabel = SettingKey[String]("Default label for project")
  val stashLocation = TaskKey[File]("Place where stashed artifacts are kept")
  val overrideExisting = SettingKey[Boolean]("Override existing stash")


  def askForLabels = Def.inputTask {
    parser.parsed match {
      case Nil => (defaultVersionLabel.value, globalLabel.value)
      case Seq(label) => (label, globalLabel.value)
      case Seq(currentProjectLabel, label) => (label, currentProjectLabel)
      case _ => throw new IllegalArgumentException("Only one args is required!") // TODO add proper parser!
    }
  }

  def doStashApplyImpl = Def.inputTask {
    val (currentLabel, currentGlobalLabel) = askForLabels.evaluated
    val location = stashLocation.value / currentGlobalLabel / currentLabel
    val setup = projectSetupFor.value

    importCacheTaskImpl(setup, CleanClasses, location.toPath)
    streams.value.log.info(s"Cache imported from ${setup.classesRoot} to use with ${setup.classesRoot}")

  }

  def doStashImpl = Def.inputTask {
    val (currentLabel, currentGlobalLabel) = askForLabels.evaluated
    val location = stashLocation.value / currentGlobalLabel / currentLabel
    if (location.exists() && location.list().nonEmpty)
      if (overrideExisting.value) IO.delete(location)
      else throw new IllegalArgumentException("Cache exist")

    location.mkdirs()

    val setup = projectSetupFor.value

    exportCacheTaskImpl(setup, location.toPath, compileIncremental.value)
    streams.value.log.info(s"Cache exported tp $location from ${setup.classesRoot}")
  }

  private def defaultSettings = Seq(
    defaultVersionLabel := "HEAD",
    globalLabel := file(".").getName,
    stashLocation := dependencyCacheDirectory.value / ".." / "sbt-stash",
    overrideExisting := true
  )

  private def perConfigSettings = Seq(doStashKey <<= doStashImpl, doStashApplyKey <<= doStashApplyImpl)

  def settings =
    inConfig(Compile)(perConfigSettings) ++ inConfig(Test)(perConfigSettings) ++ Seq(
      stashKey := {
        (doStashKey in Compile).value
        (doStashKey in Test).value
      },
      stashApplyKey := {
        (doStashApplyKey in Compile).value
        (doStashApplyKey in Test).value
      }
    ) ++ defaultSettings

}


