package sbt.inc.hoarder

import java.io.File
import java.nio.file.Path

import sbt.inc.Stamp

import scala.util.Try

case class Mapper[V](read: String => V, write: V => String)

case class ContextAwareMapper[C, V](read: (C, String) => V, write: (C, V) => String)

object Mapper {
  private val header = "##"

  val forFile: Mapper[File] = Mapper(FormatCommons.stringToFile, FormatCommons.fileToString)
  val forString: Mapper[String] = Mapper(identity, identity)
  val forStamp: ContextAwareMapper[File, Stamp] = ContextAwareMapper((_, v) => Stamp.fromString(v), (_, s) => s.toString)

  def staticFile(value: File) = {
    def read(v: String): File = if (v == header) value else FormatCommons.stringToFile(v)
    def write(f: File): String = if (f == value) header else FormatCommons.fileToString(f)

    Mapper(read, write)
  }

  private val Regexp = s"#(\\d+)#(.+)".r
  private def asNth(n: Int, v: Any) = s"#$n#$v"

  def multipleRoots(roots: Seq[Path]) = {
    def write(f: File): String = {
      val path = f.toPath
      val fromSourceRoots = roots.zipWithIndex.collectFirst {
        case (root, nr) if path.startsWith(root) =>
          asNth(nr, root.relativize(path))
      }
      fromSourceRoots.getOrElse(FormatCommons.fileToString(f))
    }

    def read(s: String): File = s match {
      case Regexp(nr, path) if nr.toInt < roots.size =>
        roots(nr.toInt).resolve(path).toFile
      case _ => FormatCommons.stringToFile(s)
    }

    Mapper(read, write)
  }

  implicit class MapperOpts[V](mapper: Mapper[V]) {
    def map[T](map: V => T, unmap: T => V) = Mapper[T](mapper.read.andThen(map), unmap.andThen(mapper.write))
  }

  def rebaseFile(from: Path, to: Path): Mapper[File] = {
    def rebaseFile(from: Path, to: Path): File => File =
      f =>
        Try(to.resolve(from.relativize(f.toPath)).toFile).getOrElse(f)

    forFile.map(rebaseFile(from, to), rebaseFile(to, from))
  }

  def relativizeFile(root: Path): Mapper[File] = {
    def write(f: File): String =
      Try {
        val relativePath = root.relativize(f.toPath).toString
        s"$header$relativePath"
      }.getOrElse(FormatCommons.fileToString(f))

    def read(string: String): File =
      if (string.startsWith(header)) root.resolve(string.drop(header.length)).toFile
      else FormatCommons.stringToFile(string)

    Mapper[File](read, write)
  }

  def updateModificationDateFileMapper(from: Mapper[File]): ContextAwareMapper[File, Stamp] =
    ContextAwareMapper(
      (binaryFile, _) => Stamp.lastModified(from.read(binaryFile.toString)),
      (_, stamp) => stamp.toString
    )
}

trait AnalysisMappers {
  val outputDirMapper: Mapper[File] = Mapper.forFile
  val sourceDirMapper: Mapper[File] = Mapper.forFile
  val scalacOptions: Mapper[String] = Mapper.forString

  val sourceMapper: Mapper[File] = Mapper.forFile
  val productMapper: Mapper[File] = Mapper.forFile
  val binaryMapper: Mapper[File] = Mapper.forFile


  val binaryStampMapper: ContextAwareMapper[File, Stamp] = Mapper.forStamp
  val productStampMapper: ContextAwareMapper[File, Stamp] = Mapper.forStamp
  val sourceStampMapper: ContextAwareMapper[File, Stamp] = Mapper.forStamp
}

object AnalysisMappers {
  val default = new AnalysisMappers {}
}