package org.romanowski.hoarder.core

import java.nio.file.Files
import java.nio.file.Path

import xsbti.compile.analysis._
import xsbti.compile._


trait CompilationCache {
  protected def cacheVerifier(): Option[CacheVerifier] = None
  protected def createMapper: ReadWriteMappers
  protected def cacheLocation: Path

  protected def importBinaries(imported: AnalysisContents): AnalysisContents
  protected def exportBinaries(from: AnalysisContents): Unit


  val analysisCacheZipFileName = "analysis.zip"
  val classesZipFileName = "classes.zip"



  def cacheExisits(): Boolean = {
    Files.exists(cacheLocation) && Files.isDirectory(cacheLocation) &&
      Files.exists(cacheLocation.resolve(analysisCacheZipFileName)) &&
      Files.exists(cacheLocation.resolve(classesZipFileName))
  }

  def loadCache(): Option[AnalysisContents] = {
    val mapper = createMapper
    val store = FileAnalysisStore.getDefault(cacheLocation.resolve(analysisCacheZipFileName).toFile, mapper)

    val content = sbt.util.InterfaceUtil.toOption(store.get())
    content.map(importBinaries)
  }
  def exportCache(from: AnalysisContents): Option[VerficationResults] = {
    val mapper = createMapper
    val verifier = cacheVerifier()
    val verifiedMapper = verifier.map(_.verifingMappers(mapper)).getOrElse(mapper)

    val store = FileAnalysisStore.getDefault(cacheLocation.resolve(analysisCacheZipFileName).toFile, mapper)
    store.set(from)
    exportBinaries(from)
    verifier.map(_.results())
  }
}