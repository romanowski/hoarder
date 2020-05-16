/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2020, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package bar

import common.CommonData

object BarDataSpec {
  val data = BarData(CommonData("bar", 42), "bar")
}
