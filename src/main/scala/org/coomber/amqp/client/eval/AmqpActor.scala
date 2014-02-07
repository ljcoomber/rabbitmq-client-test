package org.coomber.amqp.client.eval

import akka.actor.{Props, ActorSystem, Actor}
import com.github.sstone.amqp.Amqp._
import com.github.sstone.amqp.{ChannelOwner, ConnectionOwner}
import scala.Some
import concurrent.duration._

trait AmqpActor extends Actor {

  def config: BrokerConfig

  def name: String

  def initRequests: List[Request]

  lazy val channel = makeChannel()

  lazy val connection = makeConnection()


  private def connFactory(config: BrokerConfig) = {
    ConnectionOwner.buildConnFactory(host = config.host, port = config.port, vhost = config.vhost, user = config.user,
      password = config.password)
  }

  private def makeConnection() = {
    val conn = context.system.actorOf(Props(
      new ConnectionOwner(connFactory(config), reconnectionDelay = config.reconnectionDelayMillis.millis)),
      name = s"${name}Conn")
    waitForConnection(context.system, conn).await()
    conn
  }

  private def makeChannel() = {
    val chan = ConnectionOwner.createChildActor(
      connection,
      Props(new ChannelOwner(init = initRequests)),
      Some(s"${name}Chan"),
      config.makeChannelTimeoutMillis)
    waitForConnection(context.system, chan).await()
    chan
  }
}
