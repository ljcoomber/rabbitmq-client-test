package org.coomber.amqp.eval

trait MessageTracker {

  val messagePrefix = "TEST_MESSAGE_"

  var lastConfirmedMsgSeqNo = 0

  def extractSeqNo(msg: Array[Byte]): Int = {
    val n = new String(msg).substring(messagePrefix.length)
    n.toInt
  }

  def seeMessage(msg: Array[Byte]) {
    val expectedSeqNo = lastConfirmedMsgSeqNo + 1
    val actualSeqNo = extractSeqNo(msg)
    if(actualSeqNo != expectedSeqNo) println(s"Broken sequence: expected $expectedSeqNo, got $actualSeqNo")
    lastConfirmedMsgSeqNo = actualSeqNo
  }

  def failMessage(msg: Array[Byte], reason: Option[Throwable] = None) {
    val msgSeqNo = seeMessage(msg)
    println(s"Publish of message $msgSeqNo failed."
      + reason.map(r => s" Reason: ${r.getClass.getName} / ${r.getMessage}"))
  }
}
