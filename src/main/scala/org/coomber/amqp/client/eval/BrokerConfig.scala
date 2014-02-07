package org.coomber.amqp.client.eval

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
