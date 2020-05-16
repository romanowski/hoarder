/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2020, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package bar

import cats.kernel.Eq
import common.CommonData

final case class BarData(common: CommonData, bar: String)

object BarData {
  implicit val eq: Eq[BarData] = Eq.fromUniversalEquals
}
