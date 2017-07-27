package org.romanowski.hoarder.core

import java.nio.file.Path

import xsbti.compile.analysis._
import xsbti.compile._

case class ExportedCache(analysis: Path, verificationResults: Option[VerficationResults])

trait CompilationCache {
  def cacheExists(): Boolean
  def loadCache(): Option[AnalysisContents]
  def exportCache(from: AnalysisContents): ExportedCache
}
