/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package pl.typosafe.cacher

import sbt.internal.inc._
import sbt.internal.inc.cached._
import sbt.inc.cached._
import xsbti.compile._

class ProjectRebaseCacheSpec extends CommonCachedCompilation("Project based cache") {

  override def remoteCacheProvider() = new CacheProvider {
    override def findCache(previous: Option[(CompileAnalysis, MiniSetup)]): Option[CompilationCache] =
      Some(ProjectRebasedCache(remoteProject.baseLocation, remoteProject.defaultStoreLocation.toPath))
  }

}