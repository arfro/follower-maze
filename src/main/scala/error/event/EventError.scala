package error.event

trait EventError
case class EventMessageFormatError(message: String, rawEventMessage: String) extends EventError
case class EventMessageDeliveryError(message: String, rawEventMessage: String) extends EventError
