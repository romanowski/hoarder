/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

import java.io.File
import java.nio.file.Path
import sbt._

object MyCiSetup extends org.romanowski.hoarder.actions.ci.DownloadableCacheSetup {
  override def shouldPublishCaches(): Boolean = new File(".shouldPublishCaches").exists()

  override def shouldUseCache(): Boolean = new File(".shouldUseCache").exists()

  private val cache = new File(".hoadrder-cache")

  /** Loads cache to tmp directory */
  def downloadCache(to: Path): Unit = {
    IO.copyDirectory(cache, to.toFile)
  }

  /** Stores cache from tmp directory */
  def uploadCache(from: Path): Unit = {
    IO.copyDirectory(from.toFile, cache)
  }

  def invalidateCache(): Unit = IO.delete(cache)
}


object MyCiPlugin extends org.romanowski.hoarder.actions.CachedCI.PluginBase(MyCiSetup)