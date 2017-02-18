/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

import java.io.File
import java.nio.file.Path

import sbt.Attributed
import sbt.Keys._
import sbt.inc.Stamp
import sbt.internal.inc.{AnalysisMappers, ContextAwareMapper, FormatCommons, Mapper}



class SbtAnalysisMapper(sbtOutput: Path,
                        sourceRoots: Seq[Path],
                        projectRoot: Path,
                        classpath: Seq[Attributed[File]],
                        includeSize: Boolean = false) extends AnalysisMappers {
  case class JarDescriptor(org: String, rev: String, name: String, fileName: String, cl: Option[String], configuration: Option[String])

  private val header = "##"

  //artifact.key, art).put(moduleID.key, module).put(configuration.key
  def stringifyAttributes(from: Attributed[File]): Option[String] = for {
    art <- from.get(artifact.key)
    module <- from.get(moduleID.key)
  } yield
    Seq(module.organization,
      module.revision,
      module.name,
      from.data.getName,
      art.classifier.getOrElse("-"),
      module.configurations.getOrElse("-"),
      if(includeSize) from.data.length() else -1
    ).mkString("#")

  def writeDescriptor(f: File): String = {
    classpath.find(_.data == f).flatMap(stringifyAttributes)
        .getOrElse(
          if(f.toPath.startsWith(projectRoot)) s"$header${projectRoot.relativize(f.toPath).toString}"
          else FormatCommons.fileToString(f)
        )
  }

  lazy val classpathDescriptors: Map[String, File] = classpath.map(stringifyAttributes).zip(classpath).collect {
    case (Some(k), attributted) => k -> attributted.data
  }(collection.breakOut)

  def readDescriptor(s: String): File = classpathDescriptors.get(s).getOrElse(
    if(s.startsWith(header)) projectRoot.resolve(s.drop(header.size)).toFile
    else FormatCommons.stringToFile(s)
  )

  override val outputDirMapper: Mapper[File] = Mapper.relativizeFile(sbtOutput)
  override val sourceDirMapper: Mapper[File] = Mapper.multipleRoots(sourceRoots, projectRoot)
  override val sourceMapper: Mapper[File] = sourceDirMapper
  override val productMapper: Mapper[File] = Mapper.relativizeFile(sbtOutput)
  override val binaryMapper: Mapper[File] = Mapper(readDescriptor, writeDescriptor)
  override val binaryStampMapper: ContextAwareMapper[File, Stamp] =
    Mapper.updateModificationDateFileMapper(binaryMapper)
  override val productStampMapper: ContextAwareMapper[File, Stamp] =
    Mapper.updateModificationDateFileMapper(productMapper)
}