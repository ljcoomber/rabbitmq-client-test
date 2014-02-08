package org.coomber.amqp.eval

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import itv.amqp.client.eval._
import itv.amqp.client.eval.Payload
import itv.amqp.client.eval.SuccessfulPublish


class TestPublisher extends Actor with MessageTracker {

  import TestPublisher.system.dispatcher
  val config = new PublisherConfig
  val publisher = context.actorOf(Props(new ReliablePublisher(config)), name = config.publisherName)

  var seqNo = 1

  TestPublisher.system.scheduler.schedule(0 seconds, 10 milliseconds) {
    publisher ! Payload("TEST_ROUTING_KEY", s"$messagePrefix$seqNo".getBytes)
    seqNo = seqNo + 1
  }

  def receive = {
    case SuccessfulPublish(msg) => {
      seeMessage(msg)
    }
    case FailedPublish(msg, reason) => {
      failMessage(msg, Option(reason))
    }
    case unknown => println(s"Received unknown message $unknown")
  }
}

object TestPublisher extends App {

  implicit val system = ActorSystem("testPublisherSystem")

  system.actorOf(Props(new TestPublisher))
}
