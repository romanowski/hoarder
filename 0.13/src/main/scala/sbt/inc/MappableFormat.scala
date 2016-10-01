package sbt.inc

import java.io._
import javax.xml.bind.DatatypeConverter

import sbt.internal.inc._
import sbt.{CompileOptions, CompileSetup, Relation}
import xsbti.api.{Compilation, Source}
import xsbti.compile.{MultipleOutput, SingleOutput}

object DefaultFormat extends MappableFormat(AnalysisMappers.default)

class MappableFormat(val mappers: AnalysisMappers) extends RelationsTextFormat {

  // Some types are not required for external inspection/manipulation of the analysis file,
  // and are complex to serialize as text. So we serialize them as base64-encoded sbinary-serialized blobs.
  // TODO: This is a big performance hit. Figure out a more efficient way to serialize API objects?
  import AnalysisFormats._
  implicit val compilationF = xsbt.api.CompilationFormat

  def write(out: Writer, analysis: Analysis, setup: CompileSetup): Unit = {
    VersionF.write(out)
    // We start with writing compile setup which contains value of the `nameHashing`
    // flag that is needed to properly deserialize relations
    FormatTimer.time("write setup") { CompileSetupF.write(out, setup) }
    // Next we write relations because that's the part of greatest interest to external readers,
    // who can abort reading early once they're read them.
    FormatTimer.time("write relations") { RelationsF.write(out, analysis.relations) }
    FormatTimer.time("write stamps") { StampsF.write(out, analysis.stamps) }
    FormatTimer.time("write apis") { APIsF.write(out, analysis.apis) }
    FormatTimer.time("write sourceinfos") { SourceInfosF.write(out, analysis.infos) }
    FormatTimer.time("write compilations") { CompilationsF.write(out, analysis.compilations) }
    out.flush()
  }

  def read(in: BufferedReader): (Analysis, CompileSetup) = {
    VersionF.read(in)
    val setup = FormatTimer.time("read setup") { CompileSetupF.read(in) }
    val relations = FormatTimer.time("read relations") { RelationsF.read(in, setup.nameHashing) }
    val stamps = FormatTimer.time("read stamps") { StampsF.read(in) }
    val apis = FormatTimer.time("read apis") { APIsF.read(in) }
    val infos = FormatTimer.time("read sourceinfos") { SourceInfosF.read(in) }
    val compilations = FormatTimer.time("read compilations") { CompilationsF.read(in) }

    (Analysis.Empty.copy(stamps, apis, relations, infos, compilations), setup)
  }

  private[this] object VersionF {
    val currentVersion = "5"

    def write(out: Writer): Unit = {
      out.write("format version: %s\n".format(currentVersion))
    }

    private val versionPattern = """format version: (\w+)""".r
    def read(in: BufferedReader): Unit = {
      in.readLine() match {
        case versionPattern(version) => validateVersion(version)
        case s: String               => throw new ReadException("\"format version: <version>\"", s)
        case null                    => throw new EOFException
      }
    }

    def validateVersion(version: String): Unit = {
      // TODO: Support backwards compatibility?
      if (version != currentVersion) {
        throw new ReadException("File uses format version %s, but we are compatible with version %s only.".format(version, currentVersion))
      }
    }
  }

  private[this] object RelationsF {
    object Headers {
      val srcProd = "products"
      val binaryDep = "binary dependencies"
      val directSrcDep = "direct source dependencies"
      val directExternalDep = "direct external dependencies"
      val internalSrcDepPI = "public inherited source dependencies"
      val externalDepPI = "public inherited external dependencies"
      val classes = "class names"

      val memberRefInternalDep = "member reference internal dependencies"
      val memberRefExternalDep = "member reference external dependencies"
      val inheritanceInternalDep = "inheritance internal dependencies"
      val inheritanceExternalDep = "inheritance external dependencies"

      val usedNames = "used names"
    }

    def write(out: Writer, relations: Relations): Unit = {
      // This ordering is used to persist all values in order. Since all values will be
      // persisted using their string representation, it makes sense to sort them using
      // their string representation.
      val toStringOrd = new Ordering[Any] {
        def compare(a: Any, b: Any) = a.toString compare b.toString
      }
      def writeRelation[T](header: String, rel: Relation[File, T]): Unit = {
        writeHeader(out, header)
        writeSize(out, rel.size)
        // We sort for ease of debugging and for more efficient reconstruction when reading.
        // Note that we don't share code with writeMap. Each is implemented more efficiently
        // than the shared code would be, and the difference is measurable on large analyses.
        rel.forwardMap.toSeq.sortBy(_._1).foreach {
          case (k, vs) =>
            val kStr = k.toString
            vs.toSeq.sorted(toStringOrd) foreach { v =>
              out.write(kStr); out.write(" -> "); out.write(v.toString); out.write("\n")
            }
        }
      }

      relations.allRelations.foreach {
        case (header, rel) => writeRelation(header, rel)
      }
    }

    def read(in: BufferedReader, nameHashing: Boolean): Relations = {
      def readRelation[T](expectedHeader: String, s2t: String => T): Relation[File, T] = {
        val items = readPairs(in)(expectedHeader, new File(_), s2t).toIterator
        // Reconstruct the forward map. This is more efficient than Relation.empty ++ items.
        var forward: List[(File, Set[T])] = Nil
        var currentItem: (File, T) = null
        var currentFile: File = null
        var currentVals: List[T] = Nil
        def closeEntry(): Unit = {
          if (currentFile != null) forward = (currentFile, currentVals.toSet) :: forward
          currentFile = currentItem._1
          currentVals = currentItem._2 :: Nil
        }
        while (items.hasNext) {
          currentItem = items.next()
          if (currentItem._1 == currentFile) currentVals = currentItem._2 :: currentVals else closeEntry()
        }
        if (currentItem != null) closeEntry()
        Relation.reconstruct(forward.toMap)
      }

      val relations = Relations.existingRelations map { case (header, fun) => readRelation(header, fun) }

      Relations.construct(nameHashing, relations)
    }
  }

  private[this] object StampsF {
    object Headers {
      val products = "product stamps"
      val sources = "source stamps"
      val binaries = "binary stamps"
      val classNames = "class names"
    }

    def write(out: Writer, stamps: Stamps): Unit = {
      def doWriteMap[V](header: String, m: Map[File, V], keyMapper: Mapper[File], valueMapper: ContextAwareMapper[File, V]) = {
        val pairsToWrite = m.keys.toSeq.sorted map (k => (k, valueMapper.write(k, m(k))))
        writePairs(out)(header, pairsToWrite, keyMapper.write, identity[String])
      }

      doWriteMap(Headers.products, stamps.products, mappers.productMapper, mappers.productStampMapper)
      doWriteMap(Headers.sources, stamps.sources, mappers.sourceMapper, mappers.sourceStampMapper)
      doWriteMap(Headers.binaries, stamps.binaries, mappers.binaryMapper, mappers.binaryStampMapper)
      writeMap(out)(Headers.classNames, stamps.classNames, mappers.binaryMapper.write, identity[String])
    }

    def read(in: BufferedReader): Stamps = {
      def doReadMap[V](expectedHeader: String, keyMapper: Mapper[File], valueMapper: ContextAwareMapper[File, V]) =
        readMappedPairs(in)(expectedHeader, keyMapper.read, valueMapper.read).toMap

      val products = doReadMap(Headers.products, mappers.productMapper, mappers.productStampMapper)
      val sources = doReadMap(Headers.sources, mappers.sourceMapper, mappers.sourceStampMapper)
      val binaries = doReadMap(Headers.binaries, mappers.binaryMapper, mappers.binaryStampMapper)
      val classNames = readMap(in)(Headers.classNames, mappers.binaryMapper.read, identity[String])

      Stamps(products, sources, binaries, classNames)
    }
  }

  private[this] object APIsF {
    object Headers {
      val internal = "internal apis"
      val external = "external apis"
    }

    val stringToSource = ObjectStringifier.stringToObj[Source] _
    val sourceToString = ObjectStringifier.objToString[Source] _

    def write(out: Writer, apis: APIs): Unit = {
      writeMap(out)(Headers.internal, apis.internal, mappers.sourceMapper.write, sourceToString,  inlineVals = false)
      writeMap(out)(Headers.external, apis.external, Mapper.forString.write, sourceToString, inlineVals = false)
      FormatTimer.close("bytes -> base64")
      FormatTimer.close("byte copy")
      FormatTimer.close("sbinary write")
    }

    def read(in: BufferedReader): APIs = {
      val internal = readMap(in)(Headers.internal, mappers.sourceMapper.read, stringToSource)
      val external = readMap(in)(Headers.external, Mapper.forString.read, stringToSource)
      FormatTimer.close("base64 -> bytes")
      FormatTimer.close("sbinary read")
      APIs(internal, external)
    }
  }

  private[this] object SourceInfosF {
    object Headers {
      val infos = "source infos"
    }

    val stringToSourceInfo = ObjectStringifier.stringToObj[SourceInfo] _
    val sourceInfoToString = ObjectStringifier.objToString[SourceInfo] _

    def write(out: Writer, infos: SourceInfos): Unit =
      writeMap(out)(Headers.infos, infos.allInfos, mappers.sourceMapper.write, sourceInfoToString, inlineVals = false)
    def read(in: BufferedReader): SourceInfos =
      SourceInfos.make(readMap(in)(Headers.infos, mappers.sourceMapper.read, stringToSourceInfo))
  }

  private[this] object CompilationsF {
    object Headers {
      val compilations = "compilations"
    }

    val stringToCompilation = ObjectStringifier.stringToObj[Compilation] _
    val compilationToString = ObjectStringifier.objToString[Compilation] _

    def write(out: Writer, compilations: Compilations): Unit =
      writeSeq(out)(Headers.compilations, compilations.allCompilations, compilationToString)

    def read(in: BufferedReader): Compilations = Compilations.make(
      readSeq[Compilation](in)(Headers.compilations, stringToCompilation))
  }

  private[this] object CompileSetupF {
    object Headers {
      val outputMode = "output mode"
      val outputDir = "output directories"
      val compileOptions = "compile options"
      val javacOptions = "javac options"
      val compilerVersion = "compiler version"
      val compileOrder = "compile order"
      val nameHashing = "name hashing"
    }

    private[this] val singleOutputMode = "single"
    private[this] val multipleOutputMode = "multiple"
    private[this] val singleOutputKey = new File("output dir")

    def write(out: Writer, setup: CompileSetup): Unit = {
      val (mode, outputAsMap) = setup.output match {
        case s: SingleOutput   => (singleOutputMode, Map(singleOutputKey -> s.outputDirectory))
        case m: MultipleOutput => (multipleOutputMode, m.outputGroups.map(x => x.sourceDirectory -> x.outputDirectory).toMap)
      }

      writeSeq(out)(Headers.outputMode, mode :: Nil, identity[String])
      writeMap(out)(Headers.outputDir, outputAsMap, mappers.sourceDirMapper.write, mappers.outputDirMapper.write)
      writeSeq(out)(Headers.compileOptions, setup.options.options, mappers.scalacOptions.write)
      writeSeq(out)(Headers.javacOptions, setup.options.javacOptions, identity[String])
      writeSeq(out)(Headers.compilerVersion, setup.compilerVersion :: Nil, identity[String])
      writeSeq(out)(Headers.compileOrder, setup.order.name :: Nil, identity[String])
      writeSeq(out)(Headers.nameHashing, setup.nameHashing :: Nil, (b: Boolean) => b.toString)
    }

    def read(in: BufferedReader): CompileSetup = {
      def s2f(s: String) = new File(s)
      def s2b(s: String): Boolean = s.toBoolean
      val outputDirMode = readSeq(in)(Headers.outputMode, identity[String]).headOption
      val outputAsMap = readMap(in)(Headers.outputDir, mappers.sourceDirMapper.read, mappers.outputDirMapper.read)
      val compileOptions = readSeq(in)(Headers.compileOptions, mappers.scalacOptions.read)
      val javacOptions = readSeq(in)(Headers.javacOptions, identity[String])
      val compilerVersion = readSeq(in)(Headers.compilerVersion, identity[String]).head
      val compileOrder = readSeq(in)(Headers.compileOrder, identity[String]).head
      val nameHashing = readSeq(in)(Headers.nameHashing, s2b).head

      val output = outputDirMode match {
        case Some(s) => s match {
          case `singleOutputMode` => new SingleOutput {
            val outputDirectory = outputAsMap(singleOutputKey)
          }
          case `multipleOutputMode` => new MultipleOutput {
            val outputGroups: Array[MultipleOutput.OutputGroup] = outputAsMap.toArray.map {
              case (src: File, out: File) => new MultipleOutput.OutputGroup {
                val sourceDirectory = src
                val outputDirectory = out
                override def toString = s"OutputGroup($src -> $out)"
              }
            }
            override def toString = s"MultipleOuput($outputGroups)"
          }
          case str: String => throw new ReadException("Unrecognized output mode: " + str)
        }
        case None => throw new ReadException("No output mode specified")
      }

      new CompileSetup(output, new CompileOptions(compileOptions, javacOptions), compilerVersion,
        xsbti.compile.CompileOrder.valueOf(compileOrder), nameHashing)
    }
  }

  private[this] object ObjectStringifier {
    def objToString[T](o: T)(implicit fmt: sbinary.Format[T]) = {
      val baos = new ByteArrayOutputStream()
      val out = new sbinary.JavaOutput(baos)
      FormatTimer.aggregate("sbinary write") { try { fmt.writes(out, o) } finally { baos.close() } }
      val bytes = FormatTimer.aggregate("byte copy") { baos.toByteArray }
      FormatTimer.aggregate("bytes -> base64") { DatatypeConverter.printBase64Binary(bytes) }
    }

    def stringToObj[T](s: String)(implicit fmt: sbinary.Format[T]) = {
      val bytes = FormatTimer.aggregate("base64 -> bytes") { DatatypeConverter.parseBase64Binary(s) }
      val in = new sbinary.JavaInput(new ByteArrayInputStream(bytes))
      FormatTimer.aggregate("sbinary read") { fmt.reads(in) }
    }
  }
}
