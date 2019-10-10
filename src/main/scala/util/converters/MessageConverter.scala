package util.converters

import model.alias.Aliases.EventMessageRaw
import model.error.EventMessageFormatError
import model.event.{Broadcast, Event, Follow, PrivateMessage, StatusUpdate, Unfollow}

object MessageConverter {

  def convertEventMessageRawToEvent(rawEventMessage: EventMessageRaw): Either[EventMessageFormatError, Event] =
    createEvent(rawEventMessage)

  private def createEvent(rawEventMessage: String): Either[EventMessageFormatError, Event] = {
    val messageAsArray = rawEventMessage.split("\\|")

    messageAsArray match {
      case arr if !isCorrectMessageFormat(arr) =>
        Left(EventMessageFormatError(s"incorrect message format: ${rawEventMessage}", rawEventMessage))
      case arr => createEventInner(arr, rawEventMessage)
    }
  }

  private def isCorrectMessageFormat(messageAsArray: Array[String]): Boolean =
    messageAsArray match {
      case arr if arr.length > 4 || arr.length < 2 => false
      case arr if arr.contains("") => false
      case _ => true
    }

  /**
   * Improvements:
   * - wouldn't ship to prod like this as there could be messages that contain Strings e.g. abc|abc|abc|abc. This would
   * throw an exception on e.g. "abc".toLong. For prod I would add a proper error handling and parsing
   */
  private def createEventInner(messageAsArray: Array[String], rawEventMessage: EventMessageRaw): Either[EventMessageFormatError, Event] = {
    messageAsArray(1) match {
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
