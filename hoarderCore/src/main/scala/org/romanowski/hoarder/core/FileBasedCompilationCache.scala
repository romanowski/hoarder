package org.romanowski.hoarder.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

import ZincSpecific._

object FileBasedCompilationCache {
  val analysisCacheZipFileName = "analysis.zip"
  val classesZipFileName = "classes.zip"
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
      allowApplyingEmptyCache || (Files.isDirectory(cacheLocation) && Files.exists(cacheLocation)),
      s"Cache does not exists in ${cacheLocation.toAbsolutePath()}")

    importAnalysisContent().flatMap {
      original =>
        importBinaries(original)
        // We need to load cache again to update modification date
        importAnalysisContent()
    }
  }
  override def exportCache(from: AnalysisContents): ExportedCache = {
    cleanupAndPrepareCacheLocation()

    val analysisPath = cacheLocation.resolve(analysisCacheZipFileName)
    val verifier = exportAnalysisContent(from, analysisPath)
    exportBinaries(from)

    ExportedCache(analysisPath, verifier.map(_.results()))
  }

  protected def createMapper: AnalysisMapper

  protected def cacheLocation: Path

  protected def exportAnalysis(content: AnalysisContents): Unit = {

  }

  protected def importAnalysisContent(): Option[AnalysisContents] =
    createMapper.loadAnalysis(cacheLocation.resolve(analysisCacheZipFileName))

  protected def exportAnalysisContent(content: AnalysisContents, location: Path): Option[CacheVerifier] = {
    val verifier = cacheVerifier()
    val mapper = createMapper
    val verifiedMapper = verifier.map(_.verifingMappers(mapper)).getOrElse(mapper)
    verifiedMapper.storeAnalysis(location, content)
    verifier
  }

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
  }

  protected def cleanOutputMode: CleanOutputMode = CleanOutput

  protected def overrideExistingCache: Boolean = true

  protected def allowApplyingEmptyCache: Boolean = false

  protected def binariesToExport(outputDir: File, from: AnalysisContents): Seq[File] =
    outputDir findInDir "*.class" //TODO use analysis

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
            IO.delete(outputDir findInDir "*.class")
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