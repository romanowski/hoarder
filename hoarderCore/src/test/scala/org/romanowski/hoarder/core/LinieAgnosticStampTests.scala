package org.romanowski.hoarder.core

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalatest.FlatSpec
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.Matchers

class LineAgnosticStampTests extends FlatSpec with Matchers {

  //TODO add support for cross content mapper!
  val charsets = Seq(StandardCharsets.UTF_8 /*, StandardCharsets.ISO_8859_1*/)
  val endings = Map(
    "linux" -> "\n",
    "old mac" -> "\r",
    "windows" -> "\r\n")

  behavior of "LineAgnosticStamper"


  val tmpDir = Files.createTempDirectory("LineAgnosticStampTests")
  val content = Seq("ala", "ma", "", "", "kota", "w\t2", "paśki")

  def hash(lineEndings: String, charset: Charset) = {
    val path = tmpDir.resolve("test-" + System.currentTimeMillis())
    import collection.JavaConverters._
    Files.write(path, Seq(content.mkString(lineEndings)).asJava, charset)
    LineAgnosticStamp(path.toFile)
  }

  val baseHash = hash(endings.head._2, charsets.head)

  for {
    charset <- charsets
    (os, ending) <- endings
  } it should s"Hash $os line ending using ${charset.name()}" in {
    hash(ending, charset) shouldEqual baseHash
  }

  for {
    charset <- charsets
  } it should s"Hash different file differently ending using ${charset.name()}" in {
    hash("other", charset) should not equal baseHash
  }


}
