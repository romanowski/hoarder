package org.romanowski.hoarder.core

import sbt.Artifact
import sbt.Compiler
import sbt.File
import sbt.IO.transfer
import sbt.URL
import sbt.Using
import sbt.compiler.IC
import sbt.ModuleID

object SbtTypes {
  type CompilationResult = IC.Result
  type PreviousCompilationResult = Compiler.PreviousAnalysis

  def createArtifact(name: String,
                     `type`: String,
                     extension: String,
                     classifier: Option[String],
                     configurations: Seq[sbt.Configuration]) =
    Artifact(name, `type`, extension, classifier, configurations, None)

  implicit class ModuleIDOpts(m: ModuleID){

    def withConfigurations(c: Option[String]) = m.copy(configurations = c)
    def withRevision(r: String) = m.copy(revision = r)
    def withExplicitArtifacts(a: Seq[Artifact]) = m.copy(explicitArtifacts = a)
  }

  implicit class CompilationResultOpts(results: CompilationResult){
    def compiledAfter(date: Long): Set[String] =
      results.analysis.apis.internal.collect {
        case (file, source) if source.compilation().startTime() >= date =>
          file.toPath.getFileName.toString
      }(collection.breakOut)
  }

}
