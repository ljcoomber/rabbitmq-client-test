package itv.amqp.client.eval

import akka.actor.{Props, ActorSystem}
import com.github.sstone.amqp.Amqp
import com.github.sstone.amqp.Amqp.ExchangeParameters

object EvalRunner {

  implicit val system = ActorSystem("amqpSystem")

  def main(args: Array[String]) {
    val config = new PublisherConfig
    val publisher = system.actorOf(Props(new ReliablePublisher(config)), name = config.publisherName)

    (1 to 10) map (i => { publisher ! Payload("", i.toString.getBytes) } )
  }
}
