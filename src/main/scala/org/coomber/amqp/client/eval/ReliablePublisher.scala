package itv.amqp.client.eval

import com.rabbitmq.client._

import concurrent.duration._
import com.github.sstone.amqp.Amqp._

import akka.actor._
import com.github.sstone.amqp.{Amqp, ChannelOwner, ConnectionOwner}
import scala.Byte
import com.github.sstone.amqp.Amqp.DeclareExchange
import com.github.sstone.amqp.Amqp.Publish
import com.github.sstone.amqp.Amqp.ExchangeParameters
import scala.Some
import com.rabbitmq.client.AMQP.Tx.CommitOk

case class Payload(routingKey: String, contents: Array[Byte])

case class SuccessfulPublish(contents: Array[Byte])

case class FailedPublish(contents: Array[Byte], reason: Throwable)

class BrokerConfig {
  val host = "localhost"
  val port = 5672
  val vhost = "/"
  val user = "guest"
  val password = "guest"

  val connectTimeoutMillis = 5000
  val makeChannelTimeoutMillis = 1000
  val reconnectionDelayMillis = 1000
}


class PublisherConfig extends BrokerConfig {
  val publisherName = "testPublisher"

  val exchangeName = "testX"
}


class ReliablePublisher(config: PublisherConfig) extends Actor {

  import ReliablePublisher._

  val exchange = ExchangeParameters(config.exchangeName, passive = false, exchangeType = "direct", durable = true)

  lazy val channel = makeChannelOwner(context.system, config, exchange)

  override def receive = {
    case Payload(routingKey, contents) => channel !
      // Tx are slow, but sufficient for now. Look at Publisher Confirms in future
      Transaction(List(Publish(exchange.name, routingKey,
        properties = Some(PersistentDeliveryMode), mandatory = false, immediate = false, body = contents)))
    case Ok(Transaction(committedMsgs), Some(_: CommitOk)) => committedMsgs.map(m => context.parent ! SuccessfulPublish(m.body))
    case Error(Transaction(failedMsgs), reason: Throwable) => failedMsgs.map(m => context.parent ! FailedPublish(m.body, reason))
    case other => println(s"Failed to handle message: $other")
  }
}


object ReliablePublisher {

  val PersistentDeliveryMode = new AMQP.BasicProperties().builder().deliveryMode(2).build()

  private def connFactory(config: PublisherConfig) = {
    ConnectionOwner.buildConnFactory(host = config.host, port = config.port, vhost = config.vhost, user = config.user,
      password = config.password)
  }


  private def makeConnectionOwner(system: ActorSystem, config: PublisherConfig) = {
    val conn = system.actorOf(Props(
      new ConnectionOwner(connFactory(config), reconnectionDelay = config.reconnectionDelayMillis.millis)),
      name = s"${config.publisherName}Conn")
    waitForConnection(system, conn).await()
    conn
  }


  def makeChannelOwner(system: ActorSystem, config: PublisherConfig, exchange: ExchangeParameters) = {
    val chan = ConnectionOwner.createChildActor(
      makeConnectionOwner(system, config),
      Props(new ChannelOwner(init = List(DeclareExchange(exchange)))),
      Some(s"${config.publisherName}Chan"),
      config.makeChannelTimeoutMillis)
    waitForConnection(system, chan).await()
    chan
  }
}
