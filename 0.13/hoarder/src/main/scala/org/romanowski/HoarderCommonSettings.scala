package org.romanowski

import java.nio.file.Path

import org.romanowski.hoarder.core._
import sbt._


object HoarderCommonSettings {
  val globalCacheLocation = InputKey[Path]("cacheLocation", "Location for cache for given project.")
  val staticCacheLocation = TaskKey[Path]("staticCacheLocation", "Location for cache for given project.")
  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")
  val overrideExistingCache = SettingKey[Boolean]("Override existing stash")

  val defaults = Seq(cleanOutputMode := CleanClasses, overrideExistingCache := false)
}


