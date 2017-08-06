package org.romanowski.hoarder.core

import java.io.File
import java.nio.file.Path

import xsbti.compile.analysis._
import xsbti.compile._

object ZincSpecific {
  type AnalysisMapper = xsbti.compile.analysis.ReadWriteMappers
  type AnalysisContents = xsbti.compile.AnalysisContents

  val IO = sbt.io.IO


  implicit class AnalysisContentsOps(on: AnalysisContents) {
    def output(): File = {
      val singleOutput = on.getMiniSetup().output().getSingleOutput
      assert(singleOutput.isPresent(), "Hoarder can import cache only into single input!")
      singleOutput.get()
    }
  }

  implicit class AnalysisMapperOpts(mapper: ReadWriteMappers){
    private def store(path: Path) = FileAnalysisStore.getDefault(path.toFile, mapper)

    def loadAnalysis(from: Path): Option[AnalysisContents] =
      sbt.util.InterfaceUtil.toOption(store(from).get())

    def storeAnalysis(in: Path, analysis: AnalysisContents): Unit = store(in).set(analysis)
  }

  implicit class FilesOpts(f: File){
    def findInDir(pattern: String): Seq[File] =
      (sbt.io.PathFinder(f) ** pattern).get
  }

}
