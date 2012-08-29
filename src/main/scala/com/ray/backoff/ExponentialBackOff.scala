package com.ray.backoff

import util.Random
import java.util.concurrent.atomic.AtomicInteger
import akka.actor._
import akka.util.Duration
import akka.util.Duration._
import akka.util.duration._
import akka.actor.SupervisorStrategy.{Restart, Resume, Escalate}
import akka.actor.ChildRestartStats

trait ExponentialBackOff extends SupervisorStrategy with ExponentialBackOffAlgorithm {
  abstract override def handleFailure(context: ActorContext, child: ActorRef, cause: Throwable, stats: ChildRestartStats, children: Iterable[ChildRestartStats]): Boolean = {
    val directive = if (decider.isDefinedAt(cause)) decider(cause) else Escalate
    waited = directive match {
      case Resume | Restart => backOff.getOrElse(Zero)
      case _ =>
        reset()
        Zero
    }
    println("slept "+waited)
    super.handleFailure(context, child, cause, stats, children)
  }

  @volatile private var waited:Duration= Zero
  def lastWaitTime = waited
}

trait ExponentialBackOffAlgorithm {
  def slotTime: Duration =100 millis
  def ceiling: Int = 16
  def stayAtCeiling: Boolean = false

  val rand = new Random()
  val slot = new AtomicInteger(1)

  /**
   * Resets the backOff
   */
  def reset() {
    slot.set(1)
  }

  /**
   * gets the next wait time in milliseconds
   * returns the next wait value or None if the ceiling has been returned before this call (if stayAtCeiling is false).
   * if stayAtCeiling is true and at ceiling, the nextWait will be at the ceiling
   * (reset to start again)
   */
  def nextWait: Option[Duration] = {
    def time = Some(times * slotTime)
    def times = {
      val exp = rand.nextInt(slot.incrementAndGet())
      math.round(math.pow(2, exp) - 1)
    }
    if (slot.get() > ceiling) {
      if (stayAtCeiling) {
        slot.set(ceiling)
        time
      } else {
        None
      }
    } else {
      time
    }
  }

  /**
   * Sleeps on the current thread for the current slot and return the time slept as an Option,
   * or will reset the back-off when ceiling has been passed, and not sleep
   * and return None
   * @return Some(time) or None if not slept
   */
  def backOff = {
    nextWait.map { time =>
      Thread.sleep(time.toMillis)
      time
    }.orElse {
      if (!stayAtCeiling) reset()
      None
    }
  }
}
