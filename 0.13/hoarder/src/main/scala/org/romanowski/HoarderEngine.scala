package org.romanowski

import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}

import org.romanowski.hoarder.SbtAnalysisMapper
import sbt.Keys._
import sbt.{IO, _}
import sbt.compiler.{IC, MixedAnalyzingCompiler}
import sbt.inc.MappableFormat
import sbt.internal.inc.AnalysisMappers
import xsbti.compile.SingleOutput

trait HoarderEngine {
  val analysisCacheFileName = "analysis.txt"
  val classesZipFileName = "classes.zip"

  protected def createMapper(projectSetup: ProjectSetup): AnalysisMappers = {
    import projectSetup._
    new SbtAnalysisMapper(classesRoot, sourceRoots.map(_.toPath), projectRoot, classpath)
  }

  case class ProjectSetup(sourceRoots: Seq[File],
                          classpath: Classpath,
                          classesRoot: Path,
                          projectRoot: Path,
                          analysisFile: File,
                          configurationPath: Path
                         )

  protected def exportCacheTaskImpl(setup: ProjectSetup, globalCacheLocation: Path, result: IC.Result): Unit = {
    val thisCacheLocation = globalCacheLocation.resolve(setup.configurationPath)
    Files.createDirectories(thisCacheLocation)

    val mapper = createMapper(setup)
    val fos = Files.newBufferedWriter(thisCacheLocation.resolve(analysisCacheFileName), Charset.forName("UTF-8"))
    try {
      new MappableFormat(mapper).write(fos, result.analysis, result.setup)
    } finally fos.close()

    val outputPath = ouputForProject(result.setup).toPath

    val classesToZip = result.analysis.stamps.allProducts.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    IO.zip(classesToZip, thisCacheLocation.resolve(classesZipFileName).toFile)
  }

  private def ouputForProject(setup: CompileSetup): File = setup.output match {
    case s: SingleOutput =>
      s.outputDirectory()
    case _ =>
      fail("Cannot use cache in multi-output situation")
  }

  protected def importCacheTaskImpl(projectSetup: ProjectSetup, cleanOutputMode: CleanOutputMode, cacheLocation: Path) = {
    val from = cacheLocation.resolve(analysisCacheFileName)
    val classesZip = cacheLocation.resolve(classesZipFileName)
    val mapper = createMapper(projectSetup)
    val outputDir = projectSetup.classesRoot.toFile

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

      val store = MixedAnalyzingCompiler.staticCachedStore(projectSetup.analysisFile)
      store.set(analysis, setup)

      Some(Compiler.PreviousAnalysis(analysis, Some(setup)))
    } else None
  }

  protected def projectSetupFor = Def.task {
    val roots = managedSourceDirectories.value ++ unmanagedSourceDirectories.value
    val classpath = dependencyClasspath.value
    val output = classDirectory.value.toPath
    val projectRoot = baseDirectory.value.toPath
    val analysisFile = compileIncSetup.value.cacheFile
    val projectName = name.value
    val configurationName = configuration.value.name

    ProjectSetup(roots, classpath, output, projectRoot, analysisFile, Paths.get(projectName, configurationName))
  }

}
