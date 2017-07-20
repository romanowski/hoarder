package org.romanowski.hoarder.core

import sbt.Artifact
import sbt.File
import sbt.IO.transfer
import java.net.URL
import java.nio.file.Paths

import sbt.io.Using
import xsbti.compile._
import sbt.librarymanagement.ConfigRef


object SbtTypes {
  type CompilationResult = xsbti.compile.CompileResult
  type PreviousCompilationResult = PreviousResult

  implicit class IOOps(io: sbt.io.IO.type){
    def download(url: URL, to: File) =
      Using.urlInputStream(url) { inputStream =>
        transfer(inputStream, to)
      }
  }

  def createArtifact(name: String,
                    `type`: String,
                    extension: String,
                    classifier: Option[String],
                    configurations: Seq[sbt.Configuration]): Artifact =
    Artifact(name, `type`, extension, classifier, configurations.map(n => ConfigRef(n.name)).toVector, None)

  def fail(m: String) = throw new RuntimeException(m)

  implicit class CompilationResultOpts(results: CompilationResult){
    def asAnalysisContents: AnalysisContents = AnalysisContents.create(results.analysis(), results.setup())


    def compiledAfter(date: Long): Set[String] ={
      results.analysis() match {
        case analysis: sbt.internal.inc.Analysis =>
          val relations = analysis.relations
          analysis.apis.internal.collect {
            case (file, source) if source.compilationTimestamp() > date =>
              relations.definesClass(file).map(_.getName())
          }.flatten.toSet
      }
    }
  }
}
