/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.location

import org.scalatest.{FlatSpec, Matchers}
import sbt.complete.{DefaultParsers, Parser}
import sbt.complete.Parser.Value

class StaticInteractiveLocationTests extends FlatSpec with Matchers {

  implicit class ParserOps[T](opts: Parser[T]) {
    def successful(input: String): T = {
      DefaultParsers(opts)(input).resultEmpty match {
        case Value(v) =>
          v
        case failure =>
          fail(s"Input '$input' parsed with following errors: ${failure.errors}")
      }
    }

    def failed(input: String) =  DefaultParsers(opts)(input).resultEmpty shouldBe a[Parser.Failure]
  }

  behavior of "Parser"

  it should "Parse correctly" in {
    StaticInteractiveLocation.parser.successful("") shouldEqual (None, None)
    StaticInteractiveLocation.parser.successful(" ") shouldEqual (None, None)
    StaticInteractiveLocation.parser.successful("  ") shouldEqual (None, None)
    StaticInteractiveLocation.parser.successful("ala ola") shouldEqual (Some("ala"), Some("ola"))
    StaticInteractiveLocation.parser.successful("ala    ola") shouldEqual (Some("ala"), Some("ola"))
    StaticInteractiveLocation.parser.successful("  ala    ola") shouldEqual (Some("ala"), Some("ola"))
    StaticInteractiveLocation.parser.successful("ala") shouldEqual (Some("ala"), None)
    StaticInteractiveLocation.parser.successful(" ala") shouldEqual (Some("ala"), None)
    StaticInteractiveLocation.parser.successful(" ala    ") shouldEqual (Some("ala"), None)
    StaticInteractiveLocation.parser.successful("ala    ") shouldEqual (Some("ala"), None)


    StaticInteractiveLocation.parser.failed("ala ula ola")
    StaticInteractiveLocation.parser.failed("ala\tula\tola")
  }
}
