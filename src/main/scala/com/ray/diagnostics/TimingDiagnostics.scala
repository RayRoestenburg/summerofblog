package com.ray.diagnostics

case class TimeDataRequest(id:Long)

/**
 * A Diagnostics that records timing based on the Id of the message
 */
trait TimingDiagnostics extends Diagnostics[(Long,Long),TimeDataRequest] {
  private var map = Map[Long, Long]()
  var timeBefore:Long = 0

  def diagnoseBefore:Receive = {
    case m:HasId =>
      timeBefore = now
    case _ =>
  }

  def diagnoseAfter:Receive = {
    case m:HasId =>
      map = map + (m.id -> (now - timeBefore))
    case _ =>
  }

  def collect(req:TimeDataRequest) = map.get(req.id).map( (req.id, _))
}

trait HasId {
  def id:Long
}
