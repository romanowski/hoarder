package sbt.inc

import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.io.StringWriter

import org.scalacheck.Prop._
import org.scalacheck._
import sbt.CompileOptions
import sbt.CompileSetup
import xsbti.Problem
import xsbti.compile._

object DefaultFormatTest extends Properties("TextAnalysisFormat") with BaseTextAnalysisFormatTest {
  override def format = DefaultFormat
}

object DefaultFormatFromTextFormatTest extends Properties("TextAnalysisFormat") with BaseTextAnalysisFormatTest {
  override def format = DefaultFormat

  override protected def serialize(analysis: Analysis, format: MappableFormat): String = {
    val writer = new StringWriter

    TextAnalysisFormat.write(writer, analysis, commonSetup)
    writer.toString
  }
}

object TextFormatFromDefaultFormatTest extends Properties("TextAnalysisFormat") with BaseTextAnalysisFormatTest {
  override def format = DefaultFormat

  override protected def deserialize(from: String, format: MappableFormat): (Analysis, CompileSetup) = {
    val reader = new BufferedReader(new StringReader(from))

    val (readAnalysis: Analysis, readSetup) = TextAnalysisFormat.read(reader)
    (readAnalysis, readSetup)
  }
}

object TextFormatFromTextFormatTest extends Properties("TextAnalysisFormat") with BaseTextAnalysisFormatTest {
  override def format = DefaultFormat

  override protected def deserialize(from: String, format: MappableFormat): (Analysis, CompileSetup) = {
    val reader = new BufferedReader(new StringReader(from))

    val (readAnalysis: Analysis, readSetup) = TextAnalysisFormat.read(reader)
    (readAnalysis, readSetup)
  }

  override protected def serialize(analysis: Analysis, format: MappableFormat): String = {
    val writer = new StringWriter

    TextAnalysisFormat.write(writer, analysis, commonSetup)
    writer.toString
  }
}

trait BaseTextAnalysisFormatTest {
  self: Properties =>

  def format: MappableFormat

  val nameHashing = true
  val storeApis = true
  val dummyOutput = new xsbti.compile.SingleOutput {
    def outputDirectory: java.io.File = new java.io.File("/dummy")
  }
  val commonSetup = new CompileSetup(dummyOutput, new CompileOptions(Nil, Nil),
    "2.10.4", xsbti.compile.CompileOrder.Mixed, nameHashing)


  protected def serialize(analysis: Analysis, format: MappableFormat): String = {
    val writer = new StringWriter

    format.write(writer, analysis, commonSetup)
    writer.toString
  }

  protected def deserialize(from: String, format: MappableFormat): (Analysis, CompileSetup) = {
    val reader = new BufferedReader(new StringReader(from))

    val (readAnalysis: Analysis, readSetup) = format.read(reader)
    (readAnalysis, readSetup)
  }

  protected def checkAnalysis(analysis: Analysis) = {
    val (readAnalysis, readSetup) = deserialize(serialize(analysis, format), format)

    compare(analysis, readAnalysis) && compare(commonSetup, readSetup)
  }

  property("Write and read empty Analysis") = {
    checkAnalysis(Analysis.empty(nameHashing))
  }

  property("Write and read simple Analysis") = {

    import TestCaseGenerators._

    def f(s: String) = new File(s)
    val aScala = f("A.scala")
    val bScala = f("B.scala")
    val aSource = genSource("A" :: "A$" :: Nil).sample.get
    val bSource = genSource("B" :: "B$" :: Nil).sample.get
    val cSource = genSource("C" :: Nil).sample.get
    val exists = new Exists(true)
    val sourceInfos = SourceInfos.makeInfo(Nil, Nil)

    var analysis = Analysis.empty(nameHashing)
    analysis = analysis.addProduct(aScala, f("A.class"), exists, "A")
    analysis = analysis.addProduct(aScala, f("A$.class"), exists, "A$")
    analysis = analysis.addSource(aScala, aSource, exists, Nil, Nil, sourceInfos)
    analysis = analysis.addBinaryDep(aScala, f("x.jar"), "x", exists)
    analysis = analysis.addExternalDep(aScala, "C", cSource, inherited = false)

    val writer = new StringWriter

    TextAnalysisFormat.write(writer, analysis, commonSetup)

    val result = writer.toString

    val reader = new BufferedReader(new StringReader(result))

    val (readAnalysis, readSetup) = TextAnalysisFormat.read(reader)

    checkAnalysis(analysis)
  }

  property("Write and read complex Analysis") = forAllNoShrink(TestCaseGenerators.genAnalysis(nameHashing)) { analysis: Analysis =>
    val writer = new StringWriter

    TextAnalysisFormat.write(writer, analysis, commonSetup)

    val result = writer.toString

    val reader = new BufferedReader(new StringReader(result))

    val (readAnalysis, readSetup) = TextAnalysisFormat.read(reader)

    checkAnalysis(analysis)
  }

  // Compare two analyses with useful labelling when they aren't equal.
  protected def compare(left: Analysis, right: Analysis): Prop = {
    ("STAMPS" |: left.stamps =? right.stamps) &&
      ("APIS" |: left.apis =? right.apis) &&
      ("RELATIONS" |: left.relations =? right.relations) &&
      ("SourceInfos" |: mapInfos(left.infos) =? mapInfos(right.infos)) &&
      ("Whole Analysis" |: left =? right)
  }

  private def mapInfos(a: SourceInfos): Map[File, (Seq[Problem], Seq[Problem])] =
    a.allInfos.map {
      case (f, infos) =>
        f -> (infos.reportedProblems -> infos.unreportedProblems)
    }

  private def compareOutputs(left: Output, right: Output): Prop = {
    (left, right) match {
      case (l: SingleOutput, r: SingleOutput) =>
        "Single output dir" |: l.outputDirectory() =? r.outputDirectory()
      case (l: MultipleOutput, r: MultipleOutput) =>
        "Output group match" |: l.outputGroups() =? r.outputGroups()
      case _ =>
        s"Cannot compare $left with $right" |: left =? right
    }
  }

  // Compare two analyses with useful labelling when they aren't equal.
  protected def compare(left: CompileSetup, right: CompileSetup): Prop = {
    ("OUTPUT EQUAL" |: compareOutputs(left.output, right.output)) &&
      ("JAVA OPTIONS EQUAL" |: left.options.javacOptions.toVector =? right.options.javacOptions.toVector) &&
      ("SCALA OPTIONS EQUAL" |: left.options.options.toVector =? right.options.options.toVector) &&
      ("COMPILER VERSION EQUAL" |: left.compilerVersion == right.compilerVersion) &&
      ("COMPILE ORDER EQUAL" |: left.order =? right.order) &&
      ("NAMEHASHING EQUAL" |: left.nameHashing =? right.nameHashing)
  }

}