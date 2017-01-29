package org.romanowski

import java.io.FileWriter
import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import com.sun.corba.se.spi.resolver.LocalResolver
import org.romanowski.hoarder.SbtAnalysisMapper
import sbt.Keys._
import sbt.{PathFinder, _}
import sbt.compiler.{IC, MixedAnalyzingCompiler}
import sbt.inc.{MappableFormat, TextAnalysisFormat}
import xsbti.compile.SingleOutput

sealed trait CleanOutputMode

case object CleanOutput extends CleanOutputMode

case object FailOnNonEmpty extends CleanOutputMode

case object CleanClasses extends CleanOutputMode

object Hoarder {
  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  val exportCacheLocation = SettingKey[Path]("exportCacheLocation", "Where cache should be exported")
  val exportCache = TaskKey[Path]("exportCache", "Export compilation cache to predefined location")
  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")

  val importCache = TaskKey[Unit]("importCache")

  private def ouputForProject(setup: CompileSetup): File = setup.output match {
    case s: SingleOutput =>
      s.outputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }

  private def exportCacheTaskImpl(result: IC.Result,
                                  destLocation: Path,
                                  sourceRoots: Seq[File],
                                  classpath: Classpath,
                                  outputRoot: Path,
                                  projectRoot: Path): Unit = {
    Files.createDirectories(destLocation)

    val mapper = new SbtAnalysisMapper(outputRoot, sourceRoots.map(_.toPath), projectRoot, classpath)

    val fos = Files.newBufferedWriter(destLocation.resolve(analysisCacheFileName), Charset.forName("UTF-8"))
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
    } finally fos.close()

    val outputPath = ouputForProject(result.setup).toPath

    val classesToZip = result.analysis.stamps.allProducts.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    IO.zip(classesToZip, destLocation.resolve(classesZipFileName).toFile)
  }

  def exportCacheImpl = exportCache := {
    val location = exportCacheLocation.value
    val roots = (managedSourceDirectories in Compile).value ++ (unmanagedSourceDirectories in Compile).value
    val result = (compileIncremental in Compile).value
    val classpath = (dependencyClasspath in Compile).value
    val output = (target in Compile).value.toPath
    val projectRoot = baseDirectory.value.toPath

    exportCacheTaskImpl(result, location, roots, classpath, output, projectRoot)
    streams.value.log.info(s"Cache exported tp $location")

    location
  }

  private def importCacheTaskImpl(mapper: SbtAnalysisMapper,
                                  location: Path,
                                  to: File,
                                  outputDir: File,
                                  cleanOutputMode: CleanOutputMode): Option[Compiler.PreviousAnalysis] = {
    val from = location.resolve(analysisCacheFileName)
    val classesZip = location.resolve(classesZipFileName)
    if (Files.exists(from) && Files.exists(classesZip)) {

      val ios = Files.newBufferedReader(from, Charset.forName("UTF-8"))

      if (outputDir.exists()) {
        if (outputDir.isDirectory) {
          cleanOutputMode match {
            case CleanOutput =>
              if (outputDir.list().nonEmpty) IO.delete(outputDir)
            case FailOnNonEmpty =>
              if (outputDir.list().nonEmpty)
                throw new IllegalStateException(s"Output directory: $outputDir is not empty and cleanOutput is false")
            case CleanClasses =>
              val classFiles = PathFinder(outputDir) ** "*.class"
              IO.delete(classFiles.get)
          }
        } else throw new IllegalStateException(s"Output file: $outputDir is not a directory")
      }

      IO.unzip(classesZip.toFile, outputDir, preserveLastModified = true)

      val (analysis, setup) = try {
        new MappableFormat(mapper).read(ios)
      } finally ios.close()

      val store = MixedAnalyzingCompiler.staticCachedStore(to)
      store.set(analysis, setup)

      Some(Compiler.PreviousAnalysis(analysis, Some(setup)))
    } else None
  }

  def importCacheImpl = (previousCompile in Compile) := {
    val previous = (previousCompile in Compile).value
    val location = exportCacheLocation.value
    val roots = (managedSourceDirectories in Compile).value ++ (unmanagedSourceDirectories in Compile).value
    val outputDir = (classDirectory in Compile).value
    val mapper = new SbtAnalysisMapper(
      sbtOutput = (target in compile).value.toPath,
      sourceRoots = roots.map(_.toPath),
      projectRoot = baseDirectory.value.toPath,
      classpath = (dependencyClasspath in Compile).value)

    val to = compileIncSetup.in(Compile).value.cacheFile

    streams.value.log.info(s"Importing cache from: $location")
    val res = importCacheTaskImpl(mapper, location, to, outputDir, cleanOutputMode.value).getOrElse(previous)
    streams.value.log.info(s"Done")
    res
  }

  def useStaticCache = Seq(
    exportCacheLocation := target.value.toPath.getParent.resolve("cache"),
    cleanOutputMode := CleanClasses,
    exportCacheImpl,
    importCacheImpl
  )
}