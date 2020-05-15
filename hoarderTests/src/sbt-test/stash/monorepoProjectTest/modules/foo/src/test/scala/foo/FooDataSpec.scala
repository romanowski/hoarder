/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2020, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package foo

import common.CommonData

object FooDataSpec {
  val data = FooData(CommonData("foo", 42), 42)
}