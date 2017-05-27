package org.romanowski.hoarder.actions

import org.romanowski.hoarder.actions.CachedCI.Setup
import sbt.SettingKey
import sbt.TaskKey

trait CachedCiKeys {
  val preBuild = TaskKey[Unit]("preBuild", "TODO")
  val postBuild = TaskKey[Unit]("postBuild", "TODO")

  val currentSetup = SettingKey[Setup]("hoarder:currentSetup", "TODO")
}
