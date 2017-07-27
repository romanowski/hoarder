package org.romanowski.hoarder.core

import xsbti.compile.analysis._
import xsbti.compile._

trait CacheProvider {
  def findCache(previous: Option[AnalysisContents]): Option[FileBasedCompilationCache]
}
