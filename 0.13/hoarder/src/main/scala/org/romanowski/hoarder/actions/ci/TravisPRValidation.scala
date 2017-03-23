/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions
package ci

import java.nio.file.Path
import java.nio.file.Paths

import sbt.IO

case class TravisPRValidation(cacheDirectory: Path = TravisPRValidation.defaultLocation,
                              cachedBranches: Set[String] = Set.empty) extends CachedCI.Setup {

  private def currentBranch = sys.env.get("TRAVIS_BRANCH")

  private def supportedBranch = currentBranch.exists(cachedBranches.contains) || cachedBranches.isEmpty

  override def shouldPublishCaches(): Boolean =
    sys.env.get("TRAVIS_EVENT_TYPE") == Some("push") && supportedBranch

  override def shouldUseCache(): Boolean = sys.env.get("TRAVIS_EVENT_TYPE") == Some("pull_request") && supportedBranch

  override def invalidateCache(): Unit = IO.delete(cacheDirectory.toFile)

  override def exportCachePart(op: (Path) => Unit): Unit = op(cacheDirectory)

  override def loadCachePart(op: (Path) => Unit): Unit =
    op(cacheDirectory)

  override def exportCache(op: (Path) => Unit): Unit = op(cacheDirectory)

  override def loadCache(op: (Path) => Unit): Unit = op(cacheDirectory)
}

object TravisPRValidation {
  val defaultLocation = Paths.get(".hoarder-cache")

  def defaultSetup = TravisPRValidation(defaultLocation)

  class PluginBase(cacheDirectory: Path = defaultLocation, cachedBranches: Set[String] = Set.empty)
    extends CachedCI.PluginBase(TravisPRValidation(defaultLocation, cachedBranches))
}
