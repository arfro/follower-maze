package error.event

trait EventError
trait EventMessageError extends EventError
case class EventMessageExtractionError(message: String) extends EventMessageError
