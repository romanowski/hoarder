package org.romanowski.hoarder.core.internal

import java.io.File

import xsbti.compile.analysis._
import xsbti.compile._

object VerifiedAnalysisMapper {
  def create(from: ReadWriteMappers)(analyzeValue: (String, Any, Any) => Unit):ReadWriteMappers = {
    def analyzeTypedValue[T](category: String, raw: T, mapped: T): T = {
      analyzeValue(category, raw, mapped)
      mapped
    }

    val originalWriteMapper = from.getWriteMapper()
    val writeMapper = new WriteMapper {
      override def mapSourceFile(sourceFile: File): File =
        analyzeTypedValue("sourceFile", sourceFile, originalWriteMapper.mapSourceFile(sourceFile))
      override def mapBinaryFile(binaryFile: File): File =
        analyzeTypedValue("binaryFile", binaryFile, originalWriteMapper.mapBinaryFile(binaryFile))
      override def mapProductFile(productFile: File): File =
        analyzeTypedValue("productFile", productFile, originalWriteMapper.mapProductFile(productFile))

      override def mapOutputDir(outputDir: File): File =
        analyzeTypedValue("outputDir", outputDir, originalWriteMapper.mapOutputDir(outputDir))
      override def mapSourceDir(sourceDir: File): File =
        analyzeTypedValue("sourceDir", sourceDir, originalWriteMapper.mapSourceDir(sourceDir))

      override def mapClasspathEntry(classpathEntry: File): File =
        analyzeTypedValue("classpathEntry", classpathEntry, originalWriteMapper.mapSourceFile(classpathEntry))

      override def mapJavacOption(javacOption: String): String =
        analyzeTypedValue("sourceFile", javacOption, originalWriteMapper.mapJavacOption(javacOption))
      override def mapScalacOption(scalacOption: String): String =
        analyzeTypedValue("sourceFile", scalacOption, originalWriteMapper.mapScalacOption(scalacOption))

      override def mapBinaryStamp(file: File, binaryStamp: Stamp): Stamp =
        analyzeTypedValue("binaryStamp", binaryStamp, originalWriteMapper.mapBinaryStamp(file, binaryStamp))
      override def mapSourceStamp(file: File, sourceStamp: Stamp): Stamp =
        analyzeTypedValue("sourceStamp", sourceStamp, originalWriteMapper.mapSourceStamp(file, sourceStamp))
      override def mapProductStamp(file: File, productStamp: Stamp): Stamp =
        analyzeTypedValue("productStamp", productStamp, originalWriteMapper.mapProductStamp(file, productStamp))

      override def mapMiniSetup(miniSetup: MiniSetup): MiniSetup = miniSetup
    }
    new ReadWriteMappers(from.getReadMapper(), writeMapper)
  }
}
