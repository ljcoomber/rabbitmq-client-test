package org.coomber.amqp.eval

import akka.actor.{Props, Actor, ActorSystem}
import org.coomber.amqp.client.eval.{AckMessage, ConsumerConfig, ReliableConsumer}
import com.github.sstone.amqp.Amqp.Delivery

class TestConsumer extends Actor with MessageTracker {

  val config = new ConsumerConfig

  val consumer = context.actorOf(Props(new ReliableConsumer(config, self)), name = "TestConsumer")

  def receive = {
    case Delivery(_, envelope, _, msg) => {
      seeMessage(msg)
      consumer ! AckMessage(envelope)
    }
    case unknown => println(s"Received unknown message $unknown")
  }
}

object TestConsumer extends App {

  implicit val system = ActorSystem("testConsumerSystem")

  system.actorOf(Props(new TestConsumer))
}
