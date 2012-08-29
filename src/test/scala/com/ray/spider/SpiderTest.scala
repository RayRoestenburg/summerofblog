package com.ray.spider

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor._
import akka.testkit.TestKit
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.{Promise, Await}
import akka.routing.BroadcastRouter
import org.scalatest.matchers.MustMatchers
import com.ray.diagnostics._
import com.ray.diagnostics.DiagnosticData

class SpiderTest extends TestKit(ActorSystem("spider")) with WordSpec with MustMatchers with BeforeAndAfterAll {
  implicit val timeout = Timeout(10 seconds)
  "The spider " must {
    "collect data about specific events " in  {

      val printer = system.actorOf(Props(new Printer with TimingDiagnostics with WireTap {
        def listener = testActor
      }), "printer")

      def createDiagnostic = new Transformer(printer) with TimingDiagnostics

      val actor1 = system.actorOf(Props(createDiagnostic), "t-1")
      val actor2 = system.actorOf(Props(createDiagnostic), "t-2")
      val actor3 = system.actorOf(Props(createDiagnostic), "t-3")
      val routees = Vector[ActorRef](actor1, actor2, actor3)
      val router = system.actorOf(Props(createDiagnostic).withRouter(
        BroadcastRouter(routees = routees)), "router-to-transformers")
      val transformerWithRouter = system.actorOf(Props(new Transformer(router) with TimingDiagnostics), "transformer-with-router")
      val transformer = system.actorOf(Props(new Transformer(transformerWithRouter) with TimingDiagnostics),"first-transformer")

      //  Schematically, the system looks like this:
      //                                                     -> t-1 ->
      //  () -> first-transformer -> transformer-with-router -> t-2 -> printer
      //                                                     -> t-3 ->

      transformer ! SomeMessage(1,"some text to play with")
      expectMsg(SomeMessage(1,"Some text to play with"))

      val p = Promise[Seq[DiagnosticData[(Long,Long)]]]
      val future = p.future

      val returnAddress = system.actorOf(Props(new Actor {
        var results = List[DiagnosticData[(Long, Long)]]()
        def receive = {
          case m:DiagnosticData[(Long,Long)] =>
          results = results :+ m
          if (results.size == 6) p.success(results)
        }
      }))
      // you could ask any actor with TimingDiagnostics
      printer ! (TimeDataRequest(1), Spider(returnAddress))

      val timingData = Await.result(future,1 seconds)
      timingData.map(_.data._1 must be (1))
      println(timingData.mkString("\n"))
    }
  }

  override protected def afterAll() {
    super.afterAll()
    system.shutdown()
  }
}

case class SomeMessage(id:Long, text:String) extends HasId

class Transformer(next: ActorRef) extends Actor with Node{
  def receive = {
    case m:SomeMessage =>
     send (next, m.copy(text = (m.text.head.toUpper +: m.text.tail).toString))
  }
}

class Printer extends Actor {
  def receive = {
    case m:SomeMessage =>
      println(m.text)
  }
}
