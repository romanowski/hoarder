/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions
package ci

import java.nio.file.Path
import java.nio.file.Paths

import org.romanowski.hoarder.actions.CachedCI.CacheProgress
import sbt.IO

trait TravisFlow { self: CachedCI.Setup =>
  protected def cachedBranches: Set[String] = Set.empty

  private def currentBranch = sys.env.get("TRAVIS_BRANCH")

  private def supportedBranch = currentBranch.exists(cachedBranches.contains) || cachedBranches.isEmpty

  override def shouldPublishCaches(): Boolean =
    sys.env.get("TRAVIS_EVENT_TYPE") == Some("push") && supportedBranch

  override def shouldUseCache(): Boolean = sys.env.get("TRAVIS_EVENT_TYPE") == Some("pull_request") && supportedBranch

}


case class TravisPRValidation(cacheDirectory: Path = TravisPRValidation.defaultLocation,
                              override val cachedBranches: Set[String] = Set.empty) extends CachedCI.Setup with TravisFlow {
  override def invalidateCache(): Unit = IO.delete(cacheDirectory.toFile)

  private val NoopProgress = new CacheProgress {
    def done(): Unit = {}

    override def nextPart[T](op: (Path) => T): T = op(cacheDirectory)
  }

  /** `doExportCache` will export cache for whole project to provided path */
  override def exportCache(): CacheProgress = NoopProgress

  /** `doLoadCache` will load cache for whole project from provided path */
  override def loadCache(): CacheProgress = NoopProgress
}

object TravisPRValidation {
  val defaultLocation = Paths.get(".hoarder-cache")

  def defaultSetup = TravisPRValidation(defaultLocation)

  class PluginBase(cacheDirectory: Path = defaultLocation, cachedBranches: Set[String] = Set.empty)
    extends CachedCI.PluginBase(TravisPRValidation(defaultLocation, cachedBranches))

}
