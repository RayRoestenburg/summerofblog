package com.ray.spider

import akka.actor.{ActorPath, Props, ActorRef, Actor}
import java.util.UUID
import collection.mutable

/**
 * A trait that decouples the Actor from it's 'endpoints'.
 * An Actor is conceptually a node in a web of actors
 */
trait Node { actor:Actor =>
  def send(actorRef:ActorRef, m:Any) { actorRef.tell(m) }
  def reply(m:Any) { sender ! m }
  def forward(actorRef:ActorRef, m:Any) { actorRef.forward(m) }
  def actorOf(props:Props):ActorRef = actor.context.actorOf(props)
  def actorFor(actorPath:ActorPath):ActorRef = actor.context.actorFor(actorPath)
}

/**
 *  A spider which has a home, and leaves a trail on the web
 */
case class Spider(home:ActorRef, trail:WebTrail= WebTrail())

/**
 * A trail on the web.
 */
case class WebTrail(collected:Set[ActorRef]= Set(), uuid:UUID = UUID.randomUUID())

/**
 * A reference to the node in the web and it's pathways going in and coming out.
 */
case class WebNodeRef(node:ActorRef, in:List[ActorRef], out:List[ActorRef])

/**
 * A Web node, which hooks itself into a Node.
 * Every node in the spiderweb is an actor that uses a Node trait.
 * A web is formed by the trails of webnodes, through which actors are communicating with each other.
 * All the webnodes together form a web across which spiders can travel.
 */
trait WebNode[Data,Request] extends Actor with Node {

  // pathways coming into the node
  protected val in = mutable.Set[ActorRef]()
  // pathways going out of the node
  protected val out = mutable.Set[ActorRef]()
  // used to only handle a request once that travels
  // through the web
  protected var lastId:Option[UUID] = None

  def collect(req:Request): Option[Data]

  def selfNode = WebNodeRef(self, in.toList, out.toList)

  override def send(actorRef:ActorRef, m:Any) {
    recordOutput(actorRef)
    actorRef tell (m, self)
  }
  override def forward(actorRef:ActorRef, m:Any) {
    recordOutput(actorRef)
    actorRef forward m
  }
  override def actorOf(props:Props):ActorRef = {
    val actorRef = context.actorOf(props)
    recordOutput(actorRef)
    actorRef
  }
  override def reply(m:Any) {
    recordOutput(sender)
    sender ! m
  }
  def recordOutput(actorRef:ActorRef) {
    out.add(actorRef)
  }
  def recordInput(actorRef:ActorRef) {
    if (actorRef != context.system.deadLetters){
      in.add(actorRef)
    }
  }

  def wrappedReceive:Receive = {
    case m:Any if ! m.isInstanceOf[(Request,Spider)] =>
      recordInput(sender)
      before(m)
      super.receive(m)
      after(m)
  }

  abstract override def receive = handleRequest orElse wrappedReceive

  def before:Receive

  def after:Receive

  def sendSpiders(spiderHome: ActorRef, data: Data, msg: (Request,Spider), collected: Set[ActorRef]) {
    val (request, spider) = msg
    val newTrail = spider.trail.copy(collected = collected + self)
    val newSpider = spider.copy(trail = newTrail)
    in.filterNot(in => collected.contains(in)).foreach(_ ! (request,newSpider))
    out.filterNot(out => collected.contains(out)) foreach (_ ! (request,newSpider))
  }

  def handleRequest:Receive = {
    case (req:Request, spider @ Spider(home,WebTrail(collected, uuid))) if !lastId.exists(_ == uuid) =>
      lastId = Some(uuid)
      collect(req).map { data =>
        sendSpiders(home, data, (req,spider), collected)
      }
  }
}