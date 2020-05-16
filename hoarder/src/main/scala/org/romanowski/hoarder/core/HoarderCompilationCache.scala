package org.romanowski.hoarder.core

import java.nio.file.Path

import org.romanowski.HoarderKeys.CacheSetup
import xsbti.compile.analysis.ReadWriteMappers

class HoarderCompilationCache(cacheSetup: CacheSetup, globalCacheLocation: Path) extends FileBasedCompilationCache {
  override protected def createMapper: ReadWriteMappers = {
    import cacheSetup._
    SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath).mappers
  }

  override protected def cacheLocation: Path = cacheSetup.cacheLocation(globalCacheLocation)

  override protected def cleanOutputMode: CleanOutputMode = cacheSetup.cleanOutputMode

  override protected def overrideExistingCache: Boolean = cacheSetup.overrideExistingCache

  override protected def allowApplyingEmptyCache: Boolean = cacheSetup.allowApplyingEmptyCache
}