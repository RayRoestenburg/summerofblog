package com.ray.diagnostics

import akka.actor.{ActorRef, Actor}

/**
 * A trait to wire tap every message that is processed by an Actor
 * A listener is sent every message that the actor received, after
 * the actor has processed it.
 */
trait WireTap extends Actor {
  def listener: ActorRef

  abstract override def receive = {
    case m =>
      super.receive(m)
      listener ! m
  }
}
