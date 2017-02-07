package finall

import base.Base
import leaf.Leaf
import akka.actor.Actor

class Final extends Base {
  val leaf: Leaf = ???
}

class FinalActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}