package base

import akka.actor.Actor


class Base {

}

class OldActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}