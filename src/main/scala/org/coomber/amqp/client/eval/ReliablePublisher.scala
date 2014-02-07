package itv.amqp.client.eval

import com.rabbitmq.client._

import com.github.sstone.amqp.Amqp._

import akka.actor._
import scala.Byte
import com.github.sstone.amqp.Amqp.DeclareExchange
import com.github.sstone.amqp.Amqp.Publish
import com.github.sstone.amqp.Amqp.ExchangeParameters
import scala.Some
import com.rabbitmq.client.AMQP.Tx.CommitOk
import org.coomber.amqp.client.eval.{AmqpActor, BrokerConfig}

case class Payload(routingKey: String, contents: Array[Byte])

case class SuccessfulPublish(contents: Array[Byte])

case class FailedPublish(contents: Array[Byte], reason: Throwable)


class PublisherConfig extends BrokerConfig {
  val publisherName = "testPublisher"

  val exchangeName = "testX"
}


class ReliablePublisher(injectedConfig: PublisherConfig) extends AmqpActor {

  val exchange = ExchangeParameters(config.exchangeName, passive = false, exchangeType = "direct", durable = true)

  override def initRequests = List(DeclareExchange(exchange))

  override def config = injectedConfig

  override def name = injectedConfig.publisherName

  override def receive = {
    case Payload(routingKey, contents) =>
      // Tx are slow, but sufficient for now. Look at Publisher Confirms in future
      channel ! Transaction(List(Publish(exchange.name, routingKey,
        properties = Some(ReliablePublisher.PersistentDeliveryMode), mandatory = false, immediate = false, body = contents)))
    case Ok(Transaction(committedMsgs), Some(_: CommitOk)) => committedMsgs.map(m => context.parent ! SuccessfulPublish(m.body))
    case Error(Transaction(failedMsgs), reason: Throwable) => failedMsgs.map(m => context.parent ! FailedPublish(m.body, reason))
    case other => println(s"Failed to handle message: $other")
  }
}


object ReliablePublisher {
  val PersistentDeliveryMode = new AMQP.BasicProperties().builder().deliveryMode(2).build()
}
