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

  val parser = {
    import sbt.complete.Parser._
    import sbt.complete.Parsers._

    Space.* ~> token(StringBasic, "Project label").?
      .flatMap { res =>
        (Space.+ ~> token(StringBasic, "Version label")).?.map(res -> _)
      } <~ Space.*
  }


  private def configName = Def.task {
    (configuration in ThisScope).?.value.map(_.name).getOrElse("unknown")
  }

  private def askForStashLocation = Def.inputTask {
    val (providedLabel, providedVersion) = parser.parsed

    val currentGlobalLabel = providedLabel.getOrElse(globalLabel.value)
    val currentLocalLabel = providedVersion.getOrElse(defaultVersionLabel.value)

    val file = globalStashLocation.value / currentGlobalLabel / currentLocalLabel
    file.toPath
  }

  val settings = Seq(
    defaultVersionLabel := "HEAD",
    globalLabel := file(".").getAbsoluteFile.getParentFile.getName,
    globalStashLocation := BuildPaths.getGlobalBase(state.value) / "sbt-stash",
    staticCacheLocation := globalCacheLocation.toTask("").value,
    globalCacheLocation := askForStashLocation.evaluated
  )
}
