package org.romanowski.hoarder.core

import java.io.File

import org.romanowski.hoarder.core.internal.VerifiedAnalysisMapper
import ZincSpecific.AnalysisMapper

trait VerficationResults
object NoVerification extends VerficationResults

abstract class CacheVerifier {
  final def verifingMappers(from: AnalysisMapper): AnalysisMapper = VerifiedAnalysisMapper.create(from)(analyzeValue)

  def results(): VerficationResults

  protected def analyzeValue(category: String, raw: Any, mapped: Any): Unit

}
