package util.converters

import error.event.EventMessageExtractionError
import model.event.{Event, EventMessageRaw, Follow}

object MessageConverter {

  def convertEventMessageRawToEvent(rawMessage: EventMessageRaw): Either[EventMessageExtractionError, Event] =
    createEvent(rawMessage.payload)


  private def createEvent(rawMessage: String): Either[EventMessageExtractionError, Event] = {
    val messageAsArray = rawMessage.split("\\|")

    messageAsArray match { // TODO: (style) use a for comprehension here to make it more readable?
      case arr if !isCorrectMessageFormat(arr) => // TODO: (functionality) add to dead letter queue in the function that calls convertEventMessageRawToEvent
        Left(EventMessageExtractionError(s"incorrect message format: ${rawMessage}"))
      case arr => createEventInner(arr) // there can be an EventMessageExtractionError down the line!! cant just cast Right here....
    }
  }

  private def isCorrectMessageFormat(messageAsArray: Array[String]): Boolean =
    messageAsArray match {
      case arr if arr.length > 4 || arr.length < 2 => false // if more than 4 or less than 2 that means the message is incorrect
      case arr if arr.contains("") => false // if there is any empty elements in the medsage that means it's incorrecet
      case _ => true
    }

  private def createEventInner(messageAsArray: Array[String]): Either[EventMessageExtractionError, Event] = {
    // TODO: (functionality) find a safe way to pass args to case classes - guarding condition maybe?
    // TODO: (functionality) finish up this handling
    messageAsArray(1) match { // safer handle? this could throw an excpetion if out of bounds, technically only called after correctness check though
      case messageType if messageType.equalsIgnoreCase("f") => Right(Follow(messageAsArray(0), messageAsArray(2).toLong, messageAsArray(3).toLong))
      // TODO: (functionality) how can I guarantee there is no rubbish in the messageAsArray Items???
      case messageType if messageType.equalsIgnoreCase("u") => ???
      case messageType if messageType.equalsIgnoreCase("b") => ???
      case messageType if messageType.equalsIgnoreCase("p") => ???
      case messageType if messageType.equalsIgnoreCase("s") => ???
      case messageType => Left(EventMessageExtractionError(s"unknown message type: $messageType"))
    }
  }

}
