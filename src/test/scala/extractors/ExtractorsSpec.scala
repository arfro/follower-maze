package chapter2

import tests.UnitTest

// new functinoality hence a little testing
class ExtractorsSpecsSpec extends UnitTest {

  // TODO: (test) implement those tests

  "Extractors - extractEventMessage" should "extract Event of type Follow" in {}
  it should "extract Event of type Unfollow" in {}
  it should "extract Event of type Broadcast" in {}
  it should "extract Event of type Post" in {}
  it should "extract Event of type UpdateStatus" in {}
  it should "extract Event regardless of upper/lower case of the event type" in {}
  it should "return an error for a message containing more than 4 elements" in {}
  it should "return an error for a message contaninig one element" in {}
  it should "return an error for a message with a non existing event type" in {}
  it should "return an error for a message that contains empty elements" in {}
  it should "return an error for a message that has incorrect UserId" in {}
  it should "return an error for a message that has incorrect sequence" in {}
}
