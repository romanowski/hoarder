package org.romanowski.hoarder.actions

import org.romanowski.hoarder.actions.CachedCI.Setup
import sbt.InputKey
import sbt.SettingKey
import sbt.TaskKey

trait CachedCiKeys {
  val preBuild = TaskKey[Unit]("preBuild", "Task indented to be run before CI build. " +
    "It will download cache for verification build and clean up old one for post-merge builds.")

  val storeCache = InputKey[Unit]("storeCache", "Stores cache from predefined location. Accepts project and version parameters to customize location within cache.")

  val postBuild = TaskKey[Unit]("postBuild", "Task indented to be run after CI build. " +
    "It will upload cache for post-merge builds.")

  val loadCache = InputKey[Unit]("loadCache", "Loads cache from predefined location. Accepts project and version parameters to customize location within cache.")

  // Needed for tests
  private[hoarder] val cleanCiCaches = InputKey[Unit]("hoarderCleanCiCaches", "Internal hoarder task")


  val cachedCiSetup = SettingKey[Setup]("hoarder:currentSetup",
    "Current CachedCI setup object that provides context for your CI flow.")
}
