package com.ray.diagnostics

import com.ray.spider.{WebNode, Spider, WebNodeRef}
import akka.actor.{PoisonPill, ActorRef}

/**
 * A WebNode that provides diagnostic information
 * The diagnostics adds support for requests, which will be
 * used to retrieve diagnostics.
 * While the spiders are crawling the web, this trait
 * sends DiagnosticData to the home of the spiders
 *
 * @tparam Data the type of diagnostics data
 * @tparam Request the type of diagnostics request
 */
trait Diagnostics[Data, Request] extends WebNode[Data, Request] {

  override def sendSpiders(ref: ActorRef, data: Data, msg: (Request, Spider), collected: Set[ActorRef]) {
    ref ! DiagnosticData[Data](data, now, selfNode)
    super.sendSpiders(ref, data, msg, collected)
  }

  override def before = diagnoseBefore

  override def after = diagnoseAfter

  def diagnoseBefore: Receive

  def diagnoseAfter: Receive

  def now = System.nanoTime()
}

/**
 * A Diagnostic Data message, including a timestamp when the diagnosis was taken, and at which node in the web.
 */
case class DiagnosticData[Data](data:Data, timestamp:Long, nodeRef:WebNodeRef)
