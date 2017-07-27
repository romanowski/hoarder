/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import sbt.complete.Parser.Value
import sbt.complete.DefaultParsers
import sbt.complete.Parser

class StashParserTest extends FlatSpec with Matchers {

  implicit class ParserOps[T](opts: Parser[T]) {
    def successful(input: String): T = {
      DefaultParsers(opts)(input).resultEmpty match {
        case Value(v) =>
          v
        case failure =>
          fail(s"Input '$input' parsed with following errors: ${failure.errors}")
      }
    }

    def failed(input: String) = DefaultParsers(opts)(input).resultEmpty shouldBe a[Parser.Failure]
  }

  behavior of "Parser"

  it should "Parse correctly" in {
    Stash.parser.successful("") shouldEqual(None, None)
    Stash.parser.successful(" ") shouldEqual(None, None)
    Stash.parser.successful("  ") shouldEqual(None, None)
    Stash.parser.successful(" ala ola") shouldEqual(Some("ala"), Some("ola"))
    Stash.parser.successful(" ala    ola") shouldEqual(Some("ala"), Some("ola"))
    Stash.parser.successful("  ala    ola") shouldEqual(Some("ala"), Some("ola"))
    Stash.parser.successful(" ala") shouldEqual(Some("ala"), None)
    Stash.parser.successful(" ala") shouldEqual(Some("ala"), None)
    Stash.parser.successful(" ala    ") shouldEqual(Some("ala"), None)
    Stash.parser.successful(" ala    ") shouldEqual(Some("ala"), None)


    Stash.parser.failed(" ala ula ola")
    Stash.parser.failed(" ala\tula\tola")
    Stash.parser.failed("ala")
    Stash.parser.failed("ala ola")
  }
}

