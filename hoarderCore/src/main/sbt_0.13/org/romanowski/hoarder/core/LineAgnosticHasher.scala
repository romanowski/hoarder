package org.romanowski.hoarder.core

import java.io.File

import sbt.Hash
import sbt.IO
import sbt.inc.Hash
import sbt.inc.Stamp


object LineAgnosticStamp {
  def apply(file: File): Stamp = {
    val linuxEndings = IO.readLines(file).mkString("\n")
    new Hash(Hash(linuxEndings))
  }
}
