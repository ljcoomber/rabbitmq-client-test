package org.coomber.amqp.eval

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import itv.amqp.client.eval.{SuccessfulPublish, Payload, ReliablePublisher, PublisherConfig}

class TestPublisher extends Actor {

  import TestPublisher.system.dispatcher
  val config = new PublisherConfig
  val publisher = context.actorOf(Props(new ReliablePublisher(config)), name = config.publisherName)


  TestPublisher.system.scheduler.schedule(0 seconds, 1 second) {
    publisher ! Payload("TEST_ROUTING_KEY", "TEST_MESSAGE".getBytes)
  }

  def receive = {
    case SuccessfulPublish(msg) => println(s"Successfully sent msg: ${new String(msg)}")
    case unknown => System.out.println(s"Received unknown message $unknown")
  }

}

object TestPublisher {

  implicit val system = ActorSystem("amqpSystem")

  def main(args: Array[String]) {
    val testPublisher = system.actorOf(Props(new TestPublisher))
  }
}
