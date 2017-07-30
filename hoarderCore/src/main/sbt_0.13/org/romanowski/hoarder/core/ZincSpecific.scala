package org.romanowski.hoarder.core

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

import sbt.inc.hoarder.MappableFormat
import xsbti.compile._

object ZincSpecific {
  type AnalysisMapper = sbt.inc.hoarder.AnalysisMappers
  type AnalysisContents = (sbt.inc.Analysis, sbt.CompileSetup)

  val IO = sbt.IO

  implicit class AnalysisContentsOps(on: AnalysisContents) {
    def output(): File = {
      on._2.output match {
        case s: SingleOutput =>
          s.outputDirectory()
        case _ =>
          throw new RuntimeException("Cannot use cache in multi-output situation")
      }
    }
  }

  implicit class AnalysisMapperOpts(mapper: AnalysisMapper){

    private val charset = Charset.forName("UTF-8")

    def loadAnalysis(from: Path): Option[AnalysisContents] = {
      val analysisReader = Files.newBufferedReader(from, charset)
      try Option(new MappableFormat(mapper).read(analysisReader))
      finally analysisReader.close()
    }

    def storeAnalysis(in: Path, content: AnalysisContents): Unit = {
      val fos = Files.newBufferedWriter(in, charset)
      try new MappableFormat(mapper).write(fos, content._1, content._2)
      finally fos.close()
    }
  }

  implicit class FilesOpts(f: File){
    def findInDir(pattern: String): Seq[File] =
      (sbt.PathFinder(f) ** pattern).get
  }
}