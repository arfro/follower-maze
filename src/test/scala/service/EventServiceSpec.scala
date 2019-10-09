package service

import fixtures.DeadLetterQueueSpecFixture
import tests.UnitTest

// This contains only dead letter queue tests
class EventServiceSpec extends UnitTest with DeadLetterQueueSpecFixture {

  "DeadLetterQueue" should "contain the message if format is incorrect - empty Strings as parts of a message" in {

  }
  it should "contain the message if format is incorrect - empty string" in {

  }
  it should "contain the message if format is incorrect - too many parts of the message" in {

  }
  it should "contain the message if user is offline" in {

  }
  it should "be empty if message is delivered correctly" in {

  }
}
