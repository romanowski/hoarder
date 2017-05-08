/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package leaf

import base.Base
import akka.actor.Actor

class Leaf extends Base {

}

class TestActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}