/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2020, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package common

import cats.kernel.Eq

final case class CommonData(foo: String, bar: Int)

object CommonData {
  implicit val eq: Eq[CommonData] = Eq.fromUniversalEquals
}