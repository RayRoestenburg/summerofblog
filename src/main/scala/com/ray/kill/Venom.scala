package com.ray.kill

import akka.actor.{PoisonPill, ActorRef}
import com.ray.spider.{WebNode, Spider}

/**
 * Venomous spiders kill slow Actors...
 */
trait Venom extends WebNode[Poisoned, KillSlowActors] {
  var time = 0L
  var timeBefore: Long = 0

  def now = System.currentTimeMillis()

  override def sendSpiders(ref: ActorRef, data: Poisoned, msg: (KillSlowActors, Spider), collected: Set[ActorRef]) {
    super.sendSpiders(ref, data, msg, collected)
    val (kill, _) = msg
    // the least you can do is phone home that the actor has been killed.
    kill.home ! Poisoned(self)
    self ! PoisonPill
  }

  def before: Receive = {
    case m => timeBefore = now
  }

  def after: Receive = {
    case m => time = now - timeBefore
  }

  // Slower than a second and I'll kill ya!
  def collect(req: KillSlowActors): Option[Poisoned] = if (time > 1000) Some(Poisoned(self)) else None
}

case class KillSlowActors(time:Long, home:ActorRef)
case class Poisoned(actorRef:ActorRef)