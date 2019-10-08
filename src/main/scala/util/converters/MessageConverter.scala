package util.converters

import error.event.EventMessageFormatError
import model.alias.Aliases.EventMessageRaw
import model.event.{Broadcast, Event, Follow, PrivateMessage, StatusUpdate, Unfollow}

object MessageConverter {

  def convertEventMessageRawToEvent(rawEventMessage: EventMessageRaw): Either[EventMessageFormatError, Event] =
    createEvent(rawEventMessage)


  private def createEvent(rawEventMessage: String): Either[EventMessageFormatError, Event] = {
    val messageAsArray = rawEventMessage.split("\\|")

    messageAsArray match {
      case arr if !isCorrectMessageFormat(arr) =>
        Left(EventMessageFormatError(s"incorrect message format: ${rawEventMessage}", rawEventMessage))
      case arr => createEventInner(arr, rawEventMessage) // there can be an EventMessageExtractionError down the line!! cant just cast Right here....
    }
  }

  private def isCorrectMessageFormat(messageAsArray: Array[String]): Boolean =
    messageAsArray match {
      case arr if arr.length > 4 || arr.length < 2 => false // if more than 4 or less than 2 that means the message is incorrect
      case arr if arr.contains("") => false // if there is any empty elements in the medsage that means it's incorrecet
      case _ => true
    }

  private def createEventInner(messageAsArray: Array[String], rawEventMessage: EventMessageRaw): Either[EventMessageFormatError, Event] = {
    // TODO: (functionality) find a safe way to pass args to case classes - guarding condition maybe?
    // TODO: (functionality) finish up this handling
    messageAsArray(1) match { // safer handle? this could throw an excpetion if out of bounds, technically only called after correctness check though
      case messageType if messageType.equalsIgnoreCase("f") =>
        Right(Follow(messageAsArray(0).toLong, messageAsArray(2).toLong, messageAsArray(3).toLong, rawEventMessage))
      case messageType if messageType.equalsIgnoreCase("u") =>
        Right(Unfollow(messageAsArray(0).toLong, messageAsArray(2).toLong, messageAsArray(3).toLong, rawEventMessage))
      case messageType if messageType.equalsIgnoreCase("b") =>
        Right(Broadcast(messageAsArray(0).toLong, rawEventMessage))
      case messageType if messageType.equalsIgnoreCase("p") =>
        Right(PrivateMessage(messageAsArray(0).toLong, messageAsArray(2).toLong, messageAsArray(3).toLong, rawEventMessage))
      case messageType if messageType.equalsIgnoreCase("s") =>
        Right(StatusUpdate(messageAsArray(0).toLong, messageAsArray(2).toLong, rawEventMessage))
      case messageType => Left(EventMessageFormatError(s"unknown message type: $messageType", messageAsArray.mkString("|")))
    }
  }

}
