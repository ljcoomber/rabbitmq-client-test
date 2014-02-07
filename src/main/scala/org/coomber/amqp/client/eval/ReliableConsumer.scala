package org.coomber.amqp.client.eval

import akka.actor.ActorRef
import com.github.sstone.amqp.Amqp._
import com.github.sstone.amqp.Amqp.Ok
import scala.Some
import com.github.sstone.amqp.{Consumer, ConnectionOwner}
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk


case class RequestQueueInfo()

case class QueueInfo(queueName: String, messageCount: Int, consumerCount: Int)


class ConsumerConfig extends BrokerConfig {
  val consumerName = "testConsumer"

  val queueName = "testQ"
  val routingKey = "TEST_ROUTING_KEY"
  val exchangeName = "testX"
}


class ReliableConsumer(injectedConfig: ConsumerConfig, listener: ActorRef) extends AmqpActor {

  val queueParams = QueueParameters(config.queueName, passive = false, durable = true, exclusive = false, autodelete = false)

  val queueDecl = DeclareQueue(queueParams)

  val queueBind = QueueBind(queue = config.queueName, exchange = config.exchangeName, routing_key = config.routingKey)

  val consumer = ConnectionOwner.createChildActor(connection, Consumer.props(
    listener = Some(listener),
    init = List(queueDecl, queueBind)
  ), name = Some(config.consumerName))

  consumer ! AddQueue(queueParams)

  override def initRequests = List(queueDecl, queueBind)

  override def config = injectedConfig

  override def name = injectedConfig.consumerName

  override def receive = {
    case Ok(AddQueue(qParams), Some(_)) if qParams == queueParams => println(s"Ready to consume from ${config.queueName}")   // TODO: Abandon println
    case Ok(DeclareQueue(_), Some(info: DeclareOk)) => context.parent ! QueueInfo(info.getQueue, info.getMessageCount, info.getConsumerCount)
    case RequestQueueInfo() => channel ! DeclareQueue(queueParams.copy(passive = true))
    case other => println(s"Consumer: Failed to handle message: $other")
  }
}
