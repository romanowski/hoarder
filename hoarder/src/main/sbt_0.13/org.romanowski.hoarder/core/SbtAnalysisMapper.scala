/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.nio.file.Paths

import sbt.Attributed
import sbt.Keys._
import sbt.inc.Stamp
import sbt.inc.hoarder._


case class SbtAnalysisMapper(sbtOutput: Path,
                             sourceRoots: Seq[Path],
                             projectRoot: Path,
                             classpath: Seq[Attributed[File]],
                             includeSize: Boolean = false) {

  val mappers = new AnalysisMappers {
    override val outputDirMapper: Mapper[File] = Mapper.relativizeFile(sbtOutput)
    override val sourceDirMapper: Mapper[File] = Mapper.multipleRoots(sourceRoots :+ projectRoot)
    override val sourceMapper: Mapper[File] = sourceDirMapper
    override val productMapper: Mapper[File] = Mapper.relativizeFile(sbtOutput)
    override val binaryMapper: Mapper[File] = Mapper(readDescriptor, writeDescriptor)

    override val sourceStampMapper: ContextAwareMapper[File, Stamp] =
      LineEndingAgnosticSources.mapper
    override val binaryStampMapper: ContextAwareMapper[File, Stamp] =
      Mapper.updateModificationDateFileMapper(binaryMapper)
    override val productStampMapper: ContextAwareMapper[File, Stamp] =
      Mapper.updateModificationDateFileMapper(productMapper)
  }

  private val header = "##"

  private lazy val cpMapper = new SbtClasspathMapper(projectRoot, classpath, includeSize)

  private def writeDescriptor(f: File): String = {
    cpMapper.write(f).getOrElse(
        if (f.toPath.startsWith(projectRoot)) s"$header${projectRoot.relativize(f.toPath).toString}"
        else FormatCommons.fileToString(f)
      )
  }

  private def readDescriptor(s: String): File = cpMapper.read(s).getOrElse(
    if (s.startsWith(header)) projectRoot.resolve(s.drop(header.size)).toFile
    else FormatCommons.stringToFile(s)
  )
}