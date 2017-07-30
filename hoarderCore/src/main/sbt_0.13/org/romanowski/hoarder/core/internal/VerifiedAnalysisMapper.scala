package org.romanowski.hoarder.core
package internal

import java.io.File

import ZincSpecific._

object VerifiedAnalysisMapper {
  def create(from: AnalysisMapper)(analyzeValue: (String, Any, Any) => Unit): AnalysisMapper = {
    //TODO implement!
    from
  }
}
