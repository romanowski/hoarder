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
import sbt.internal.inc.{AnalysisMappers, ContextAwareMapper, FormatCommons, Mapper}


class SbtAnalysisMapper(sbtOutput: Path,
                        sourceRoots: Seq[Path],
                        projectRoot: Path,
                        classpath: Seq[Attributed[File]],
                        includeSize: Boolean = false) extends AnalysisMappers {

	case class JarDescriptor(org: String, rev: String, name: String, fileName: String, cl: Option[String], configuration: Option[String])

	private val header = "##"

	def stringifyAttributes(from: Attributed[File]): Option[String] = for {
		artifact <- from.get(artifact.key)
		module <- from.get(moduleID.key)
	} yield
		Seq(module.organization,
			module.revision,
			module.name,
			from.data.getName,
			artifact.classifier.getOrElse("-"),
			module.configurations.getOrElse("-"),
			if (includeSize) from.data.length() else -1
		).mkString("#")

	def asJVMJar(filePath: Path): Option[String] = for {
		javaHome <- Option(System.getProperty("java.home"))
		javaHomePath = Paths.get(javaHome)
		if filePath.startsWith(javaHomePath)
		javaVersion <- Option(System.getProperty("java.specification.version"))
		relativePath = javaHomePath.relativize(filePath)
	} yield s"##$javaVersion##$relativePath"

	def writeDescriptor(f: File): String = {
		classpath.find(_.data == f).flatMap(stringifyAttributes)
			.orElse(asJVMJar(f.toPath))
			.getOrElse(
				if (f.toPath.startsWith(projectRoot)) s"$header${projectRoot.relativize(f.toPath).toString}"
				else FormatCommons.fileToString(f)
			)
	}

	lazy val jvmClasspath: Map[String, File] = if(ManagementFactory.getRuntimeMXBean.isBootClassPathSupported)
		ManagementFactory.getRuntimeMXBean.getBootClassPath.split(File.pathSeparator).flatMap { s =>
			val path = Paths.get(s)
			val jvmJar = asJVMJar(path)
			jvmJar.map( _ -> path.toFile)
		}(collection.breakOut)
		else Map.empty

	lazy val classpathDescriptors: Map[String, File] =
		jvmClasspath ++ classpath.map(stringifyAttributes).zip(classpath).collect {
			case (Some(k), attributted) => k -> attributted.data
		}(collection.breakOut)

	def readDescriptor(s: String): File = classpathDescriptors.getOrElse(s,
		if (s.startsWith(header)) projectRoot.resolve(s.drop(header.size)).toFile
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