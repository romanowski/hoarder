/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2020, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package foo

import cats.kernel.Eq
import common.CommonData

final case class FooData(common: CommonData, foo: Int)

object BarData {
  implicit val eq: Eq[FooData] = Eq.fromUniversalEquals
}
