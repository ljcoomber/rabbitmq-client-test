package org.coomber.amqp.eval

import akka.actor._
import akka.testkit.TestKit
import org.scalatest._
import akka.testkit.ImplicitSender
import org.coomber.amqp.client.eval._
import com.github.sstone.amqp.Amqp._
import scala.concurrent.duration._
import com.rabbitmq.client.Envelope
import com.github.sstone.amqp.Amqp.Publish
import org.coomber.amqp.client.eval.AckMessage
import org.coomber.amqp.client.eval.RequestQueueInfo
import com.github.sstone.amqp.Amqp.PurgeQueue
import com.github.sstone.amqp.Amqp.Ok
import com.github.sstone.amqp.Amqp.Error
import org.coomber.amqp.client.eval.QueueInfo
import com.github.sstone.amqp.Amqp.Delivery


class ConsumerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  val testMessage = "TEST_MESSAGE".getBytes

  val fixtureActor = system.actorOf(Props(new AmqpActor {
    def config: ConsumerConfig = new ConsumerConfig

    def receive = {
      case msg: Request => channel ! msg
      case Ok(PurgeQueue(q), result) => testActor ! Ok(PurgeQueue(q), result)
      case msg: Ok => // ignore
      case Error(request, reason) => println(s"ERROR: $reason sending $request}")  // TODO: Best way to propagate error
    }
  }))


  // TODO: the consumer currently passes queue info messages to its parent. The convoluted forwarding below may
  // be a sign of a bad approach, also see PublisherSpec
  var consumer: ActorRef = _  // TODO: Another sign that the approach is wrong
  val parent = system.actorOf(Props(new Actor {
    consumer = context.actorOf(Props(new ReliableConsumer(config, testActor)), name = "TestConsumer")

    def receive = {
      case m => testActor forward m
    }
  }))


  // TODO: Look at why plain BeforeAndAfter didn't mix-in correctly
  override def beforeEach() {
    fixtureActor ! PurgeQueue(config.queueName)
    expectMsgPF(){ case Ok(PurgeQueue(_), _) => true }
  }

  val config = new ConsumerConfig

  def this() = this(ActorSystem("ConsumerSpec"))

  "A consumer" must {

    "not ack messages automatically" in {
      fixtureActor ! Publish(config.exchangeName, config.routingKey, testMessage)

      val envelope = expectMsgPF() {
        case Delivery(consumerTag, envelope, _, msg) => {
          msg should equal(testMessage)
          envelope
        }
      }

      // TODO: Check number of unacked messages using REST API

      // Shows other things can be done before ack-ing received message
      consumer ! RequestQueueInfo()

      expectMsgPF() {
        case QueueInfo(_, msgCount, _) => msgCount should equal(0)
      }

      consumer ! AckMessage(envelope)
    }

    "must respect buffer limits" in {
      1 to (2 * config.prefetchCount) map { i =>
        fixtureActor ! Publish(config.exchangeName, config.routingKey, s"$TestPublisher.messagePrefix$i".getBytes)
      }

      var envelopes = List[Envelope]()

      def collectDeliveries() {
        def collectDelivery() {
          expectMsgPF(){
            case Delivery(_, envelope, _, _) => envelopes = envelope :: envelopes
          }
        }

        1 to config.prefetchCount map { _ => collectDelivery() }
      }

      collectDeliveries()

      expectNoMsg(1 second)

      envelopes = envelopes.dropWhile { env =>
        consumer ! AckMessage(env)
        true
      }

      collectDeliveries()

      expectNoMsg(1 second)

      envelopes map { consumer ! AckMessage(_) }
    }

    "allow re-delivery of nacked messages" in {
      1 to 3 map { i =>
        fixtureActor ! Publish(config.exchangeName, config.routingKey, s"$TestPublisher.messagePrefix$i".getBytes)
      }

      def collectDelivery() = expectMsgPF() { case delivery: Delivery => delivery }

      val del1 = collectDelivery()
      val del2 = collectDelivery()
      consumer ! NackMessage(del2.envelope)
      consumer ! AckMessage(del1.envelope)
      val del3 = collectDelivery()
      val del2_2 = collectDelivery()
      del2_2.body should equal (del2.body)
      consumer ! AckMessage(del2_2.envelope)
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}