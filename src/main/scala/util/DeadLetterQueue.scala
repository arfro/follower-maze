package util

import model.alias.Aliases.EventMessageRaw

object DeadLetterQueue {

  /**
   * Improvements:
   * - would not use vars, getters or setters in production, this is just to simulate storage of erroneous messages
   * **/
  private var deadLetterQueue: List[EventMessageRaw] = List()

  def addToDeadLetterQueue(rawEvent: EventMessageRaw): Unit = {
    println(s"Cannot deliver message. Added to DLQ: ${rawEvent}")
    deadLetterQueue = deadLetterQueue :+ rawEvent
  }

  def getDeadLetterQueue: List[EventMessageRaw] = deadLetterQueue

}
