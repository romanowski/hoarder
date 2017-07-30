package org.romanowski.hoarder.core

import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.nio.file.Paths

import sbt.Attributed
import sbt.Keys.artifact
import sbt.Keys.moduleID

class SbtClasspathMapper(projectRoot: Path,
                         classpath: Seq[Attributed[File]],
                         includeSize: Boolean = false) {

  def read(descriptor: String): Option[File] = classpathDescriptors.get(descriptor)

  def write(f: File): Option[String] =
    classpath.find(_.data == f).flatMap(stringifyAttributes)
    .orElse(asJVMJar(f.toPath))

  private def stringifyAttributes(from: Attributed[File]): Option[String] = for {
    artifact <- from.get(artifact.key)
    module <- from.get(moduleID.key)
  } yield {
    Seq(module.organization,
      module.revision,
      module.name,
      from.data.getName,
      artifact.classifier.getOrElse("-"),
      module.configurations.getOrElse("-"),
      if (includeSize) from.data.length() else -1
    ).mkString("#")
  }

  private def asJVMJar(filePath: Path): Option[String] = for {
    javaHome <- Option(System.getProperty("java.home"))
    javaHomePath = Paths.get(javaHome)
    if filePath.startsWith(javaHomePath)
    javaVersion <- Option(System.getProperty("java.specification.version"))
    relativePath = javaHomePath.relativize(filePath)
  } yield s"##$javaVersion##$relativePath"


  private lazy val jvmClasspath: Map[String, File] = if (ManagementFactory.getRuntimeMXBean.isBootClassPathSupported)
    ManagementFactory.getRuntimeMXBean.getBootClassPath.split(File.pathSeparator).flatMap { s =>
      val path = Paths.get(s)
      val jvmJar = asJVMJar(path)
      jvmJar.map(_ -> path.toFile)
    }(collection.breakOut)
  else Map.empty

  private lazy val classpathDescriptors: Map[String, File] =
    jvmClasspath ++ classpath.map(stringifyAttributes).zip(classpath).collect {
      case (Some(k), attributted) => k -> attributted.data
    }(collection.breakOut)
}
