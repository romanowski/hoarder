package sbt.inc

import java.io.BufferedReader
import java.io.File
import java.io.Writer

import sbt.Relation
import sbt.internal.inc._


trait RelationsTextFormat extends FormatCommons {
  def mappers: AnalysisMappers

  private case class Descriptor[B](
                                    header: String,
                                    selectCorresponding: Relations => Relation[File, B],
                                    keyMapper: Mapper[File],
                                    valueMapper: Mapper[B],
                                    enabled: Boolean = true
                                  )


  private def namesFromRelation(r: Relations): Relation[File, String] =
    r.allRelations.last._2.asInstanceOf[Relation[File, String]]

  private def allRelations(nameHashing: Boolean): List[Descriptor[_]] = {
    List(
      Descriptor("products", _.srcProd, mappers.sourceMapper, mappers.productMapper),
      Descriptor("binary dependencies", _.binaryDep, mappers.sourceMapper, mappers.binaryMapper),
      Descriptor("direct source dependencies", _.direct.internal, mappers.sourceMapper, mappers.sourceMapper, enabled = nameHashing == false),
      Descriptor("direct external dependencies", _.direct.internal, mappers.sourceMapper, mappers.sourceMapper, enabled = nameHashing == false),
      Descriptor("public inherited source dependencies", _.publicInherited.internal, mappers.sourceMapper, mappers.sourceMapper, enabled = nameHashing == false),
      Descriptor("public inherited external dependencies", _.publicInherited.external, mappers.sourceMapper, Mapper.forString, enabled = nameHashing == false),
      Descriptor("member reference internal dependencies", _.memberRef.internal, mappers.sourceMapper, mappers.sourceMapper),
      Descriptor("member reference external dependencies", _.memberRef.external, mappers.sourceMapper, Mapper.forString),
      Descriptor("inheritance internal dependencies", _.inheritance.internal, mappers.sourceMapper, mappers.sourceMapper),
      Descriptor("inheritance external dependencies", _.inheritance.external, mappers.sourceMapper, Mapper.forString),
      Descriptor("class names", _.classes, mappers.sourceMapper, Mapper.forString),
      Descriptor("used names", namesFromRelation, mappers.sourceMapper, Mapper.forString)
    )
  }

  object RelationsF {

    def write(out: Writer, relations: Relations, nameHashing: Boolean): Unit = {
      def writeRelation[B](relDesc: Descriptor[B], relations: Relations): Unit = {
        // This ordering is used to persist all values in order. Since all values will be
        // persisted using their string representation, it makes sense to sort them using
        // their string representation.
        val toStringOrdA = new Ordering[File] {
          def compare(a: File, b: File) = relDesc.keyMapper.write(a) compare relDesc.keyMapper.write(b)
        }
        val toStringOrdB = new Ordering[B] {
          def compare(a: B, b: B) = relDesc.valueMapper.write(a) compare relDesc.valueMapper.write(b)
        }

        val header = relDesc.header
        val rel: Relation[File, B] = if (relDesc.enabled) relDesc.selectCorresponding(relations) else Relation.empty[File, B]

        writeHeader(out, header)
        writeSize(out, rel.size)
        // We sort for ease of debugging and for more efficient reconstruction when reading.
        // Note that we don't share code with writeMap. Each is implemented more efficiently
        // than the shared code would be, and the difference is measurable on large analyses.
        rel.forwardMap.toSeq.sortBy(_._1)(toStringOrdA).foreach {
          case (k, vs) =>
            val kStr = relDesc.keyMapper.write(k)
            vs.toSeq.sorted(toStringOrdB) foreach { v =>
              out.write(kStr); out.write(" -> "); out.write(relDesc.valueMapper.write(v)); out.write("\n")
            }
        }
      }

      allRelations(nameHashing).foreach { relDesc => writeRelation(relDesc, relations) }
    }

    def read(in: BufferedReader, nameHashing: Boolean): Relations = {
      def readRelation[B](relDesc: Descriptor[B]): Relation[File, B] = {
        val expectedHeader = relDesc.header
        val items = readPairs(in)(expectedHeader, relDesc.keyMapper.read, relDesc.valueMapper.read).toIterator
        // Reconstruct the forward map. This is more efficient than Relation.empty ++ items.
        var forward: List[(File, Set[B])] = Nil
        var currentItem: (File, B) = null
        var currentKey: File = null
        var currentVals: List[B] = Nil
        def closeEntry(): Unit = {
          if (currentKey != null) forward = (currentKey, currentVals.toSet) :: forward
          currentKey = currentItem._1
          currentVals = currentItem._2 :: Nil
        }
        while (items.hasNext) {
          currentItem = items.next()
          if (currentItem._1 == currentKey) currentVals = currentItem._2 :: currentVals else closeEntry()
        }
        if (currentItem != null) closeEntry()
        Relation.reconstruct(forward.toMap)
      }

      val relations = allRelations(nameHashing).map(rd => readRelation(rd))

      Relations.construct(nameHashing, relations)
    }
  }

}
