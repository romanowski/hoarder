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
  val globalCacheLocation = InputKey[Path]("cacheLocation", "Location for cache for given project.")
  private[romanowski] val globalCacheLocationScoped =
    InputKey[Path]("cacheLocationScoped", "Location for cache for given project.")

  val staticCacheLocation = TaskKey[Path]("staticCacheLocation", "Location for cache for given project.")
  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")
  val overrideExistingCache = SettingKey[Boolean]("overrideExistingCache", "Override existing stash")

  def defaultsGlobal =
    Seq(
      cleanOutputMode := CleanClasses,
      overrideExistingCache := false,
      globalCacheLocationScoped.in(ImportConfig) := {
        val globalCache = globalCacheLocation.evaluated
        assert(Files.isDirectory(globalCache) && Files.exists(globalCache),
          s"Cache does not exists in $globalCache (${new File(".").getAbsolutePath}!")
        globalCache
      },
      globalCacheLocationScoped.in(ExportConfig) := {
        val globalCache = globalCacheLocation.evaluated
        assert(!Files.exists(globalCache) || overrideExistingCache.value, s"Cache already exists in $globalCache!")
        globalCache
      }
    )

  val ImportConfig = config("cacheImport")
  val ExportConfig = config("cacheExport")
}


