package org.romanowski.hoarder.core

import java.io.File

import xsbti.compile.analysis._
import xsbti.compile._


trait VerficationResults
object NoVerification extends VerficationResults

abstract class CacheVerifier {
  final def verifingMappers(from: ReadWriteMappers): ReadWriteMappers =
    new ReadWriteMappers(from.getReadMapper(), verifingWriteMappers(from.getWriteMapper()))

  private def verifingWriteMappers(from: WriteMapper): WriteMapper = {
    new WriteMapper {
      override def mapSourceFile(sourceFile: File): File =
        analyzeValueInner("sourceFile", sourceFile, from.mapSourceFile(sourceFile))
      override def mapBinaryFile(binaryFile: File): File =
        analyzeValueInner("binaryFile", binaryFile, from.mapBinaryFile(binaryFile))
      override def mapProductFile(productFile: File): File =
        analyzeValueInner("productFile", productFile, from.mapProductFile(productFile))

      override def mapOutputDir(outputDir: File): File =
        analyzeValueInner("outputDir", outputDir, from.mapOutputDir(outputDir))
      override def mapSourceDir(sourceDir: File): File =
        analyzeValueInner("sourceDir", sourceDir, from.mapSourceDir(sourceDir))

      override def mapClasspathEntry(classpathEntry: File): File =
        analyzeValueInner("classpathEntry", classpathEntry, from.mapSourceFile(classpathEntry))

      override def mapJavacOption(javacOption: String): String =
        analyzeValueInner("sourceFile", javacOption, from.mapJavacOption(javacOption))
      override def mapScalacOption(scalacOption: String): String =
        analyzeValueInner("sourceFile", scalacOption, from.mapScalacOption(scalacOption))

      override def mapBinaryStamp(file: File, binaryStamp: Stamp): Stamp =
        analyzeValueInner("binaryStamp", binaryStamp, from.mapBinaryStamp(file, binaryStamp))
      override def mapSourceStamp(file: File, sourceStamp: Stamp): Stamp =
        analyzeValueInner("sourceStamp", sourceStamp, from.mapSourceStamp(file, sourceStamp))
      override def mapProductStamp(file: File, productStamp: Stamp): Stamp =
        analyzeValueInner("productStamp", productStamp, from.mapProductStamp(file, productStamp))

      override def mapMiniSetup(miniSetup: MiniSetup): MiniSetup = miniSetup
    }
  }


  def results(): VerficationResults

  private def analyzeValueInner[T](category: String, raw: T, mapped: T): T = {
    analyzeValue(category, raw, mapped)
    mapped
  }

  protected def analyzeValue(category: String, raw: Any, mapped: Any): Unit

}
