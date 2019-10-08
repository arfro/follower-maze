package service

import fixtures.DeadLetterQueueSpecFixture
import model.alias.Aliases.EventMessageRaw
import model.error.EventMessageFormatError
import model.event._
import tests.UnitTest
import util.converters.MessageConverter

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// This contains only dead letter queue tests
class EventServiceSpec extends UnitTest with DeadLetterQueueSpecFixture {

  "DeadLetterQueue - extractEventMessage" should "extract Event of type Follow" in {
//    val sada = Await.ready(eventService.eventsAsync, Duration.Inf)
//    val expected = 2313
//    MessageConverter.convertEventMessageRawToEvent("123") shouldBe Right(expected)
  }
  it should "extract Event of type Unfollow" in {
//    val raw: EventMessageRaw = "231|U|222|55"
//    val expected = Unfollow(231, 222, 55, raw)
//    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
}
