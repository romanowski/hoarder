/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package base

import akka.actor.Actor


class Base {

}

class OldActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}