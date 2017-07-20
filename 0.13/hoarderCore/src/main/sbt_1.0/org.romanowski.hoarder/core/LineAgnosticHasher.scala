package org.romanowski.hoarder.core

import java.io.File

import sbt.io.IO
import sbt.hoarder.StampBridge
import xsbti.compile.analysis.Stamp


object LineAgnosticStamp {
  def apply(file: File): Stamp = {
    val linuxEndings = IO.readLines(file).mkString("\n")
    StampBridge.hashFileContent(linuxEndings)
  }
}
