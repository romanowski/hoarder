package org.romanowski.hoarder.actions

import org.romanowski.hoarder.actions.CachedCI.Setup
import sbt.SettingKey
import sbt.TaskKey

trait CachedCiKeys {
  val preBuild = TaskKey[Unit]("preBuild", "Task indented to be run before CI build. " +
    "It will download cache for verification build and clean up old one for post-merge builds.")

  val postBuild = TaskKey[Unit]("postBuild", "Task indented to be run after CI build. " +
    "It will upload cache for post-merge builds.")

  private[hoarder] val cleanCiCaches = TaskKey[Unit]("hoarder-cleanCiCaches", "Internal hoarder task")

  val cachedCiSetup = SettingKey[Setup]("hoarder:currentSetup",
    "Current CachedCI setup object that provides context for your CI flow.")
}
