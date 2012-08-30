package com.ray.backoff

import org.scalatest.WordSpec
import akka.actor._
import akka.util.duration._
import akka.actor.SupervisorStrategy.Restart
import akka.util.Duration
import akka.actor.AllForOneStrategy
import akka.actor.OneForOneStrategy
import akka.dispatch.Resume

//TODO Not ready yet, finish this test

object Strategies {
  val oneForOne = new OneForOneStrategy(10, Duration.Inf)({
    case _: Exception             ⇒ Restart
    //case _: NumberFormatException => Resume
  }) with ExponentialBackOff { self => OneForOneStrategy
    override def slotTime = 100 millis
    override def stayAtCeiling = true
    override def ceiling = self.maxNrOfRetries
  }

  val allForOne = new AllForOneStrategy(10, Duration.Inf)({
    case _: Exception ⇒ Restart
  }) with ExponentialBackOff { self => AllForOneStrategy
    override def slotTime = 10 millis
    override def stayAtCeiling = true
    override def ceiling = self.maxNrOfRetries
  }
}

class ExponentialBackOffTest extends WordSpec {

  "A One for One supervision strategy with exponential back-off" must {
    "wait according to back-off algorithm before restarting" in {
      val sys = ActorSystem("test")
      val supervisor = sys.actorOf(Props[OneForOneSupervisor], "Supervisor-one-for-one")
      (1 to 10).foreach(i => supervisor ! 5)
      (1 to 2).foreach(i =>supervisor ! "bla")
      (1 to 10).foreach(i => supervisor ! 5)
      (1 to 3).foreach(i =>supervisor ! "test")
      (1 to 10).foreach(i => supervisor ! 5)
      Thread.sleep(20000)
      sys.shutdown()
    }
  }

  "A All for One supervision strategy with exponential back-off" must {
    "wait according to back-off algorithm before restarting" in {


    }
  }


}
class Supervised extends Actor {


  override def preRestart(reason: Throwable, message: Option[Any]) {
    super.preRestart(reason, message)
    println("PreStart"+message)
  }

  def receive = {
    case msg:Int =>
      println(msg)

    case _ =>throw new NumberFormatException("not a number")
  }
}

class OneForOneSupervisor extends Actor {
  private var child:ActorRef = _
  override def supervisorStrategy = Strategies.oneForOne

  override def preStart() {
    super.preStart()
    child = context.actorOf(Props[Supervised])
  }

  def receive = {
    case msg => child forward msg
  }
}
