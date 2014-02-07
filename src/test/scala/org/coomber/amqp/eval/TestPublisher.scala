package org.coomber.amqp.eval

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import itv.amqp.client.eval._
import itv.amqp.client.eval.Payload
import itv.amqp.client.eval.SuccessfulPublish

class TestPublisher extends Actor {

  val messagePrefix = "TEST_MESSAGE_"

  import TestPublisher.system.dispatcher
  val config = new PublisherConfig
  val publisher = context.actorOf(Props(new ReliablePublisher(config)), name = config.publisherName)

  var i = 1

  TestPublisher.system.scheduler.schedule(0 seconds, 100 milliseconds) {
    publisher ! Payload("TEST_ROUTING_KEY", s"$messagePrefix$i".getBytes)
    i = i + 1
  }


  var lastSeenMsgSeqNo = 0

  def receive = {
    case SuccessfulPublish(msg) => {
      seeMessage(msg)
    }
    case FailedPublish(msg, reason) => {
      val msgSeqNo = seeMessage(msg)
      println(s"Publish of message $msgSeqNo failed. Reason: ${reason.getClass.getName} / ${reason.getMessage}")
    }
    case unknown => println(s"Received unknown message $unknown")
  }
  
  private def seeMessage(msg: Array[Byte]): Int = {
    val n = new String(msg).substring(messagePrefix.length)
    val msgSeqNo = n.toInt

    val expectedMsgSeqNo = lastSeenMsgSeqNo + 1
    if(msgSeqNo != expectedMsgSeqNo) println(s"Broken sequence: expected $expectedMsgSeqNo, got $msgSeqNo")
    lastSeenMsgSeqNo = msgSeqNo

    msgSeqNo
  }
}

object TestPublisher {

  implicit val system = ActorSystem("amqpSystem")

  def main(args: Array[String]) {
    val testPublisher = system.actorOf(Props(new TestPublisher))
  }
}
