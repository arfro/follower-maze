package util

import model.alias.Aliases.EventMessageRaw

import scala.collection.mutable.Queue

object DeadLetterQueue {

  /**
   * Improvements:
   * - would not use vars, getters or setters, this is just to simulate storage of erroneous messages in a queue
   ***/
  private var deadLetterQueue: Queue[EventMessageRaw] = Queue()

  def addToDeadLetterQueue(rawEvent: EventMessageRaw): Unit = {
    println(s"Cannot deliver message. Adding to DLQ: ${rawEvent}")
    deadLetterQueue = deadLetterQueue :+ rawEvent
  }

  def getDeadLetterQueue: Queue[EventMessageRaw] = deadLetterQueue

}
