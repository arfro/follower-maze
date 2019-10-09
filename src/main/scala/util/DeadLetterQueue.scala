package util

import model.alias.Aliases.EventMessageRaw

import scala.collection.mutable.Queue

object DeadLetterQueue {

  /**
   * Improvements:
   * - would not use vars, getters or setters in production, this is just to simulate storage of erroneous messages
   ***/
  private var deadLetterQueue: Queue[EventMessageRaw] = Queue()

  def addToDeadLetterQueue(rawEvent: EventMessageRaw): Unit = {
    println(s"Cannot deliver message. Adding to DLQ: ${rawEvent}")
    deadLetterQueue = deadLetterQueue :+ rawEvent
  }

  def getDeadLetterQueue: Queue[EventMessageRaw] = deadLetterQueue

}
