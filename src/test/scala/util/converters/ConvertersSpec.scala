package chapter2

import model.alias.Aliases.EventMessageRaw
import model.error.EventMessageFormatError
import model.event.{Broadcast, Follow, PrivateMessage, StatusUpdate, Unfollow}
import tests.UnitTest
import util.converters.MessageConverter


class ConvertersSpecsSpec extends UnitTest {

  "MessageConverters - extractEventMessage" should "extract Event of type Follow" in {
    val raw: EventMessageRaw = "231|F|222|55"
    val expected = Follow(231, 222, 55, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "extract Event of type Unfollow" in {
    val raw: EventMessageRaw = "231|U|222|55"
    val expected = Unfollow(231, 222, 55, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "extract Event of type Broadcast" in {
    val raw: EventMessageRaw = "231|B"
    val expected = Broadcast(231, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "extract Event of type PrivateMessage" in {
    val raw: EventMessageRaw = "231|P|2345|223"
    val expected = PrivateMessage(231, 2345, 223, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "extract Event of type StatusUpdate" in {
    val raw: EventMessageRaw = "231|S|2345"
    val expected = StatusUpdate(231, 2345, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "extract Event regardless of upper/lower case of the event type" in {
    val raw: EventMessageRaw = "231|p|2345|223"
    val expected = PrivateMessage(231, 2345, 223, raw)
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe Right(expected)
  }
  it should "return an error for a message containing more than 4 elements" in {
    val raw: EventMessageRaw = "231|p|2345|223|ew|33"
    val expected = Left(EventMessageFormatError(s"incorrect message format: $raw", raw))
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe expected
  }
  it should "return an error for a message containing one element" in {
    val raw: EventMessageRaw = "234"
    val expected = Left(EventMessageFormatError(s"incorrect message format: $raw", raw))
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe expected
  }
  it should "return an error for a message with a non existing event type" in {
    val raw: EventMessageRaw = "231|x|2345|223"
    val expected = Left(EventMessageFormatError(s"unknown message type: x", raw))
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe expected
  }
  it should "return an error for a message that contains empty elements" in {
    val raw: EventMessageRaw = "231|x||223"
    val expected = Left(EventMessageFormatError(s"incorrect message format: $raw", raw))
    MessageConverter.convertEventMessageRawToEvent(raw) shouldBe expected
  }
  ignore should "return an error for a message that has a String UserId" in {
    // such test would fail. This would be probably my biggest to-do improvement.
    // Check MessageConverter for details
  }
}
