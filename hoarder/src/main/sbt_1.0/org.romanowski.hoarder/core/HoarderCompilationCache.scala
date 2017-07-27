package org.romanowski.hoarder.core

import java.nio.file.Path

import org.romanowski.HoarderKeys.CacheSetup

class HoarderCompilationCache(cacheSetup: CacheSetup, globalCacheLocation: Path) extends FileBasedCompilationCache {
  override protected def createMapper = {
    import cacheSetup._
    SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath).mappers
  }

  override protected def cacheLocation: Path = cacheSetup.cacheLocation(globalCacheLocation)

  override protected def cleanOutputMode = cacheSetup.cleanOutputMode

  override protected def overrideExistingCache = cacheSetup.overrideExistingCache
}
