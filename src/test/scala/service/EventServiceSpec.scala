package service

import fixtures.DeadLetterQueueSpecFixture
import tests.UnitTest
import util.DeadLetterQueue

// This contains only dead letter queue tests
class EventServiceSpec extends UnitTest with DeadLetterQueueSpecFixture {

  "DeadLetterQueue" should "contain the message if format is incorrect - empty Strings as parts of a message" in {
    val message = "||f|34|22"
    val userId = "4"
    sendMessageToClientSocket(userId)
    sendMessageToEventSocket(message)
    run()
    DeadLetterQueue.getDeadLetterQueue should contain (message)
  }
  it should "contain the message if format is incorrect - empty string" in {
    val message = ""
    val userId = "4"
    sendMessageToClientSocket(userId)
    sendMessageToEventSocket(message)
    run()
    DeadLetterQueue.getDeadLetterQueue should contain (message)
  }
  it should "contain the message if format is incorrect - too many parts of the message" in {
    val message = "213|f|123|123|124"
    val userId = "4"
    sendMessageToClientSocket(userId)
    sendMessageToEventSocket(message)
    run()
    DeadLetterQueue.getDeadLetterQueue should contain (message)
  }
  it should "contain the message if target user is offline" in {
    val userId = "4"
    val message = s"1|f|$userId|124"
    sendMessageToClientSocket(userId)
    sendMessageToEventSocket(message)
    run()
    DeadLetterQueue.getDeadLetterQueue should contain (message)
  }
  it should "be not contain the message if it is delivered correctly" in {
    val userId = "4"
    val secondUserId = "44"
    val message = s"1|f|$userId|$secondUserId"
    sendMessageToClientSocket(userId)
    sendMessageToClientSocket(secondUserId)
    sendMessageToEventSocket(message)
    run()
    DeadLetterQueue.getDeadLetterQueue shouldNot contain (message)
  }
}
