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