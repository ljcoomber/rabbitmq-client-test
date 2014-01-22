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

case class Payload(routingKey: String, contents: Array[Byte])

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


// TODO: Multiple channels
// TODO: Publisher confirms
class ReliablePublisher(config: PublisherConfig) extends Actor {

  val exchange = ExchangeParameters(config.exchangeName, passive = false, exchangeType = "direct", durable = true)

  lazy val channel = ReliablePublisher.makeChannel(context.system, config, exchange)


  // TODO: Handle more messages
  override def receive = {
    case Payload(routingKey, contents) => channel ! Publish(exchange.name, routingKey,
      properties = Some(ReliablePublisher.PersistentDeliveryMode), mandatory = true, immediate = false, body = contents)
    case other => println(s"Failed to handle message: $other")
  }
}


object ReliablePublisher {

  val PersistentDeliveryMode = new AMQP.BasicProperties().builder().deliveryMode(2).build()

  private def connFactory(config: PublisherConfig) = {
    ConnectionOwner.buildConnFactory(host = config.host, port = config.port, vhost = config.vhost, user = config.user,
      password = config.password)
  }


  /**
   * Create a "connection owner" actor, which will attempt to re-connect
   */
  private def connection(system: ActorSystem, config: PublisherConfig) = {
    val conn = system.actorOf(Props(
      new ConnectionOwner(connFactory(config), reconnectionDelay = config.reconnectionDelayMillis.millis)),
      name = s"${config.publisherName}Conn")
    waitForConnection(system, conn).await()
    conn
  }


  def makeChannel(system: ActorSystem, config: PublisherConfig, exchange: ExchangeParameters) = {
    val chan = ConnectionOwner.createChildActor(
      connection(system, config),
      Props(new ChannelOwner(init = List(DeclareExchange(exchange)))),
      Some(s"${config.publisherName}Chan"),
      config.makeChannelTimeoutMillis)
    waitForConnection(system, chan).await()
    chan
  }
}
