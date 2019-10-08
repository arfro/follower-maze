package util.converters

import model.alias.Aliases.EventMessageRaw


object DeadLetterQueue {

  var deadLetterQueue: List[EventMessageRaw] = List() // i dont want to use var!!!

  def addToDeadLetterQueue(rawEvent: EventMessageRaw): Unit =
    deadLetterQueue = deadLetterQueue :+ rawEvent

}
