/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.location

import org.romanowski.HoarderCommonSettings._
import sbt.Def._
import sbt.Keys._
import sbt._

object StaticInteractiveLocation {
  val defaultVersionLabel = SettingKey[String]("Default label for project")
  val globalLabel = SettingKey[String]("Default label for project")
  val globalStashLocation = TaskKey[File]("globalStashLocation", "Place where stashed artifacts are kept")

  val parser = spaceDelimited("<export-label>")

  private def configName = Def.task {
    (configuration in ThisScope).?.value.map(_.name).getOrElse("unknown")
  }

  private def askForStashLocation = Def.inputTask {
    val args = parser.parsed

    val (currentGlobalLabel, currentLocalLabel) = args match {
      case Nil => (globalLabel.value, defaultVersionLabel.value)
      case Seq(label) => (label, defaultVersionLabel.value)
      case Seq(global, local) => (global, local)
      case _ => throw new IllegalArgumentException("Only one args is required!") // TODO add proper parser!
    }

    val file = globalStashLocation.value / currentGlobalLabel / currentLocalLabel
    file.toPath
  }

  val settings = Seq(
    defaultVersionLabel := "HEAD",
    globalLabel := file(".").getAbsoluteFile.getParentFile.getName,
    globalStashLocation := dependencyCacheDirectory.value / ".." / "sbt-stash",
    staticCacheLocation := globalCacheLocation.toTask("").value,
    globalCacheLocation := askForStashLocation.evaluated
  )
}
