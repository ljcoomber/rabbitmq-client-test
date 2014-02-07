package org.coomber.amqp.eval

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest._
import akka.testkit.ImplicitSender
import itv.amqp.client.eval.{ReliablePublisher, Payload}
import org.coomber.amqp.client.eval.{BrokerConfig, AmqpActor, ReliableConsumer, ConsumerConfig}
import com.github.sstone.amqp.Amqp._
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

  // TODO: Look at why plain BeforeAndAfter didn't mix-in correctly
  override def beforeEach() {
    fixtureActor ! PurgeQueue(config.queueName)
  }

  val config = new ConsumerConfig

  def this() = this(ActorSystem("ConsumerSpec"))

  "A consumer" must {

    "receive messages " in {
      fixtureActor ! Publish(config.exchangeName, config.routingKey, testMessage)
      val consumer = system.actorOf(Props(new ReliableConsumer(config, testActor)))

      expectMsgPF() {
        case Delivery(consumerTag, enveloper, props, msg) => msg should equal(testMessage)
      }
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}