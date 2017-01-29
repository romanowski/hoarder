package base

import akka.actor.Actor


class BaseTest {

}

class OldActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}