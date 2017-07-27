package org.romanowski.hoarder.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

import sbt.io._
import xsbti.compile.analysis._
import xsbti.compile._


object FileBasedCompilationCache {
  val analysisCacheZipFileName = "analysis.zip"
  val classesZipFileName = "classes.zip"

  implicit class AnalysisContentsOps(on: AnalysisContents) {
    def output(): File = {
      val singleOutput = on.getMiniSetup().output().getSingleOutput
      assert(singleOutput.isPresent(), "Hoarder can import cache only into single input!")
      singleOutput.get()
    }
  }
}

trait FileBasedCompilationCache extends CompilationCache {
  import FileBasedCompilationCache._

  override def cacheExists(): Boolean = {
    Files.exists(cacheLocation) && Files.isDirectory(cacheLocation) &&
      Files.exists(cacheLocation.resolve(analysisCacheZipFileName)) &&
      Files.exists(cacheLocation.resolve(classesZipFileName))
  }

  override def loadCache(): Option[AnalysisContents] = {
    assert(
      Files.isDirectory(cacheLocation) && Files.exists(cacheLocation),
      s"Cache does not exists in ${cacheLocation.toAbsolutePath()}")

    val mapper = createMapper
    val store = FileAnalysisStore.getDefault(cacheLocation.resolve(analysisCacheZipFileName).toFile, mapper)

    val content = sbt.util.InterfaceUtil.toOption(store.get())
    content.flatMap {
      original =>
        importBinaries(original)
        // We need to load cache again to update modification date
        sbt.util.InterfaceUtil.toOption(store.get())
    }
  }
  override def exportCache(from: AnalysisContents): ExportedCache = {
    val mapper = createMapper
    val verifier = cacheVerifier()
    val verifiedMapper = verifier.map(_.verifingMappers(mapper)).getOrElse(mapper)

    cleanupAndPrepareCacheLocation()

    val analysisPath = cacheLocation.resolve(analysisCacheZipFileName)
    val store = FileAnalysisStore.getDefault(analysisPath.toFile, verifiedMapper)
    store.set(from)
    exportBinaries(from)

    ExportedCache(analysisPath, verifier.map(_.results()))
  }

  protected def createMapper: ReadWriteMappers

  protected def cacheLocation: Path

  protected def cacheVerifier(): Option[CacheVerifier] = None

  protected def importBinaries(imported: AnalysisContents): Unit = {
    val output = imported.output()
    prepareImportDirectory(output)
    IO.unzip(cacheLocation.resolve(classesZipFileName).toFile, output, preserveLastModified = true)
  }

  protected def exportBinaries(from: AnalysisContents): Unit = {
    val output = from.output()
    val outputPath = output.toPath

    val classes = binariesToExport(output, from)
    val classesToZip = classes.map { classFile =>
      val mapping = outputPath.relativize(classFile.toPath).toString
      classFile -> mapping
    }

    val zippedBinaries = cacheLocation.resolve(classesZipFileName)
    IO.zip(classesToZip, zippedBinaries.toFile)

    from.getAnalysis()
  }

  protected def cleanOutputMode: CleanOutputMode = CleanOutput
  protected def overrideExistingCache: Boolean = true


  protected def binariesToExport(outputDir: File, from: AnalysisContents): Seq[File] =
    (PathFinder(outputDir) ** "*.class").get //TODO use analysis

  protected def prepareImportDirectory(outputDir: File): Unit = {
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
  }

  protected def cleanupAndPrepareCacheLocation(): Unit = {
    assert(!Files.exists(cacheLocation) || overrideExistingCache, s"Cache already exists in $cacheLocation!")

    if (Files.exists(cacheLocation)) {
      if (overrideExistingCache) IO.delete(cacheLocation.toFile)
      else new IllegalArgumentException(s"Cache already exists at $cacheLocation.")
    }

    Files.createDirectories(cacheLocation)
  }
}