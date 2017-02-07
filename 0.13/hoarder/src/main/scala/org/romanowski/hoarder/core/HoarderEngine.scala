package org.romanowski.hoarder.core

import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import sbt.Keys._
import sbt.compiler.{IC, MixedAnalyzingCompiler}
import sbt.inc.MappableFormat
import sbt.internal.inc.AnalysisMappers
import sbt.{CompileSetup, Compiler, Def, IO, PathFinder, _}
import xsbti.compile.SingleOutput
import org.romanowski.HoarderCommonSettings._

trait HoarderEngine {
  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  protected def createMapper(projectSetup: CacheSetup): AnalysisMappers = {
    import projectSetup._
    new SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath)
  }

  case class CacheSetup(sourceRoots: Seq[File],
                        classpath: Classpath,
                        classesRoot: Path,
                        projectRoot: Path,
                        analysisFile: File,
                        cacheLocation: Path,
                        overrideExistingCache: Boolean,
                        cleanOutputMode: CleanOutputMode
                       )

  protected def exportCacheTaskImpl(setup: CacheSetup, result: IC.Result): Unit = {
    import setup._

    if (Files.exists(cacheLocation)) {
      if(overrideExistingCache) IO.delete(setup.cacheLocation.toFile)
      else new IllegalArgumentException(s"Cache already exists at $cacheLocation.")
    }

    Files.createDirectories(cacheLocation)

    val mapper = createMapper(setup)
    val fos = Files.newBufferedWriter(cacheLocation.resolve(analysisCacheFileName), Charset.forName("UTF-8"))
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
    } finally fos.close()

    val outputPath = ouputForProject(result.setup).toPath

    val classes = (PathFinder(classesRoot.toFile) ** "*.class").get
    val classesToZip = classes.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    IO.zip(classesToZip, cacheLocation.resolve(classesZipFileName).toFile)
  }

  private def ouputForProject(setup: CompileSetup): File = setup.output match {
    case s: SingleOutput =>
      s.outputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }

  protected def importCacheTaskImpl(cacheSetup: CacheSetup) = {
    import cacheSetup._

    val from = cacheLocation.resolve(analysisCacheFileName)
    val classesZip = cacheLocation.resolve(classesZipFileName)
    val mapper = createMapper(cacheSetup)
    val outputDir = classesRoot.toFile

    if (Files.exists(from) && Files.exists(classesZip)) {

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

      val ios = Files.newBufferedReader(from, Charset.forName("UTF-8"))

      val (analysis, setup) = try {
        new MappableFormat(mapper).read(ios)
      } finally ios.close()

      val store = MixedAnalyzingCompiler.staticCachedStore(analysisFile)
      store.set(analysis, setup)

    //  TimeUnit.SECONDS.sleep(10)
      Some(Compiler.PreviousAnalysis(analysis, Some(setup)))
    } else None
  }

  protected def projectSetupFor = Def.task[Path => CacheSetup] {
    path => {
      CacheSetup(
        sourceRoots = managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
        classpath = externalDependencyClasspath.value,
        classesRoot = classDirectory.value.toPath,
        projectRoot = baseDirectory.value.toPath,
        analysisFile = (streams in compileIncSetup).value.cacheDirectory / compileAnalysisFilename.value,
        cacheLocation = path.resolve(name.value).resolve(configuration.value.name),
        overrideExistingCache = overrideExistingCache.value,
        cleanOutputMode = cleanOutputMode.value
      )
    }
  }

}
