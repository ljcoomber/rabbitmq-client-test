package org.coomber.amqp.eval

import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest._
import akka.testkit.ImplicitSender
import itv.amqp.client.eval.{ReliablePublisher, Payload}
import org.coomber.amqp.client.eval._
import com.github.sstone.amqp.Amqp._
import com.github.sstone.amqp.Amqp.Delivery
import com.github.sstone.amqp.Amqp.Publish
import com.github.sstone.amqp.Amqp.PurgeQueue
import com.github.sstone.amqp.Amqp.Ok
import com.github.sstone.amqp.Amqp.Error
import com.github.sstone.amqp.Amqp.Delivery


class ConsumerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  val testMessage = "TEST_MESSAGE".getBytes

  val fixtureActor = system.actorOf(Props(new AmqpActor {
    def config: BrokerConfig = new BrokerConfig

    def receive = {
      case msg: Request => channel ! msg
      case Ok(_, _) => // do nothing
      case Error(request, reason) => println(s"ERROR: $reason sending $request}")  // TODO: Best way to propagate error
    }
  }))


  // TODO: the consumer currently passes queue info messages to its parent. The convoluted forwarding below may
  // be a sign of a bad approach, also see PublisherSpec
  var consumer: ActorRef = _  // TODO: Another sign that the approach is wrong
  val parent = system.actorOf(Props(new Actor {
    consumer = context.actorOf(Props(new ReliableConsumer(config, testActor)))

    def receive = {
      case m => println("HELLO" + m); testActor forward m
    }
  }))


  // TODO: Look at why plain BeforeAndAfter didn't mix-in correctly
  override def beforeEach() {
    fixtureActor ! PurgeQueue(config.queueName)
  }

  val config = new ConsumerConfig

  def this() = this(ActorSystem("ConsumerSpec"))

  "A consumer" must {

    "not ack messages automatically" in {
      fixtureActor ! Publish(config.exchangeName, config.routingKey, testMessage)

      expectMsgPF() {
        case Delivery(consumerTag, enveloper, props, msg) => msg should equal(testMessage)
      }

      consumer ! RequestQueueInfo()

      expectMsgPF() {
        case QueueInfo(_, msgCount, _) => msgCount should equal(0)
      }
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}