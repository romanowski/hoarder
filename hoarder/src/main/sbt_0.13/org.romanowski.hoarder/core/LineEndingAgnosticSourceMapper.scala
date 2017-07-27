/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File

import sbt.Hash
import sbt.IO
import sbt.inc.Hash
import sbt.inc.Stamp
import sbt.internal.inc.ContextAwareMapper

object LineEndingAgnosticSources {

  def lineAgnosticHash(file: File): Stamp = {
    val linuxEndings = IO.readLines(file).mkString("\n")
    new Hash(Hash(linuxEndings))
  }

  def loadCacheAndCompare(file: File, serialized: String): Stamp = {
    val loadedStamp = Stamp.fromString(serialized)

    if (file.exists() && lineAgnosticHash(file) == loadedStamp) new Hash(Hash.apply(file))
    else loadedStamp
  }

  val mapper: ContextAwareMapper[File, Stamp] = ContextAwareMapper(
    read = loadCacheAndCompare,
    write = (file, stamp) => if (file.exists()) lineAgnosticHash(file).toString() else stamp.toString()
  )
}
