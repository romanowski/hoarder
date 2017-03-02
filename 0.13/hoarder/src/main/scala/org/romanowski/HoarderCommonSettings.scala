/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski

import java.nio.file.{Files, Path}

import org.romanowski.hoarder.core._
import sbt._


object HoarderCommonSettings {
  val staticCacheLocation = TaskKey[Path]("staticCacheLocation", "Location for cache for given project.")
  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")
  val overrideExistingCache = SettingKey[Boolean]("overrideExistingCache", "Override existing stash")

  def defaultsGlobal =
    Seq(
      cleanOutputMode := CleanClasses,
      overrideExistingCache := false
    )
}


