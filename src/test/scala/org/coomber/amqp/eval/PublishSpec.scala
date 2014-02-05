package org.coomber.amqp.eval

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import itv.amqp.client.eval.{SuccessfulPublish, Payload, PublisherConfig, ReliablePublisher}


class PublisherSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val testMessage = "TEST_MESSAGE".getBytes

  val config = new PublisherConfig

  def this() = this(ActorSystem("PublisherSpec"))

  "A publisher" must {

    "inform parent when messages are successfully published" in {

      // TODO: the publisher currently passes success messages to its parent. The convoluted forwarding below are
      // necessary to be able to test within TestKit, which leads me to believe that it may be a bad approach
      val parent = system.actorOf(Props(new Actor {

        val publisher = context.actorOf(Props(new ReliablePublisher(config)), name = config.publisherName)
        publisher ! Payload("TEST_ROUTING_KEY", testMessage)

        def receive = {
          case m => testActor forward m
        }
      }))

      expectMsgPF() {
        case SuccessfulPublish(msg) => msg should equal(testMessage)
      }
    }

  }


  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}