package leaf

import base.BaseTest
import akka.actor.Actor

class Test extends BaseTest {

}

class TestActor extends Actor {
  override def receive = {
    case _ => println("Ala")
  }
}