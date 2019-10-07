val message = "sadas|sdad|asdas|asda|ad"


def isCorrectMessageFormat(messageAsArray: Array[String]): Boolean =
  messageAsArray match {
    case arr if arr.length > 4 || arr.length < 2 => false // if more than 4 or less than 2 that means the message is incorrect
    case arr if arr.contains("") => false
    case _ => true
  }

isCorrectMessageFormat(message.split("\\|"))

"dsads".equalsIgnoreCase("dsads")


type UserId = String

val asda: UserId = "sad"

def sada(sda: UserId) = println(sda)

sada("asda")
