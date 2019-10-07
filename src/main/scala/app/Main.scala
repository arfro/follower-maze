package app

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import scala.collection.JavaConverters._
import module.{ConfigModule, EventModule, SocketModule}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

object Main extends App with ConfigModule with SocketModule with EventModule {

  // TODO: (functionality) add optional port numbers in config so that they can be changed by inline args when starting the application
  // TODO: (style) fix config file not to be all over the place, use format like: app { bla = 2, bla2 = 4 }


  // In main:
  // 1. Hook up single source socket
  // 2. Hook up async socket
  // 3. Loop until stopped, I guess?

  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE
  // ---------------------------------------------- OLD CODE

  override def main(args: Array[String]): Unit = {

    var lastSeqNo = 0L // not vars!
     // does it have to be a TrieMap? Why?

    implicit val ec = ExecutionContext.global

    // ------------- 1  event source
    val eventsAsync = Future { // event module
      // TODO: (functionality) kinda shady Future is used like this?

      println(s"Listening for events on ${applicationConfig.eventPort}")
      // new socket for each event?
      val eventSocket = socketService.eventSocket.accept() // TODO: (read) ServerSocket.accept

      // remove exceptions throwing maybe use either?
      Try { // TODO: (functionality) handling input in Scala correctly also read about BufferedReader - is there a better way to read from Socket?
        // TODO: (read) BufferedReader
        val reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()))

        Try {
          reader.lines().iterator().asScala.foreach { payload =>
            println(s"Message received: $payload") // TODO: (functionality) add a logger
            val message = payload.split("\\|").toList // extract splitter as a separate function
            // actually this could be a case class that holds all of this


            eventService.messagesBySeqNo += message(0).toLong -> message

            while (eventService.messagesBySeqNo.get(lastSeqNo + 1L).isDefined) { // functional way to do while loop?
              val nextMessage = eventService.messagesBySeqNo(lastSeqNo + 1)

              eventService.messagesBySeqNo -= lastSeqNo + 1L

              val nextPayload = nextMessage.mkString("|")
              val seqNo = nextMessage(0).toLong
              val kind = nextMessage(1)

              kind match {
                case "F" => // ADT instead of a string
                  val fromUserId = nextMessage(2).toLong
                  val toUserId = nextMessage(3).toLong
                  val followers = eventService.followRegistry.getOrElse(toUserId, Set.empty)
                  val newFollowers = followers + fromUserId

                  eventService.followRegistry.put(toUserId, newFollowers)

                  socketService.clientPool.get(toUserId).foreach { socket => // TODO: (read) is there a better way to write to socket?
                    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
                    writer.write(s"$nextPayload\n")
                    writer.flush()
                  }

                case "U" =>
                  val fromUserId = nextMessage(2).toLong
                  val toUserId = nextMessage(3).toLong
                  val followers = eventService.followRegistry.getOrElse(toUserId, Set.empty)
                  val newFollowers = followers - fromUserId

                  eventService.followRegistry.put(toUserId, newFollowers)

                case "P" =>
                  val toUserId = nextMessage(3).toLong

                  socketService.clientPool.get(toUserId).foreach { socket =>
                    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
                    writer.write(s"$nextPayload\n")
                    writer.flush()
                  }

                case "B" =>
                  socketService.clientPool.values.foreach { socket =>
                    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
                    writer.write(s"$nextPayload\n")
                    writer.flush()
                  }

                case "S" =>
                  val fromUserId = nextMessage(2).toLong
                  val followers = eventService.followRegistry.getOrElse(fromUserId, Set.empty)

                  followers.foreach { follower =>
                    socketService.clientPool.get(follower).foreach { socket =>
                      val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
                      writer.write(s"$nextPayload\n")
                      writer.flush()
                    }
                  }
              }

              lastSeqNo = seqNo // update as you copy!
            }
          }
        }
        if (reader != null) reader.close() // NPE handling here
      }
      if (eventSocket != null) eventSocket.close() // NPE handling here
    }

    // ------------- N user-clients
    val clientsAsync = Future {

      println(s"Listening for client requests on ${applicationConfig.clientPort}") // logger instead of print
      val serverSocket = socketService.usersSocket
      println(s"server socket: $serverSocket")
      var maybeClientSocket = Option(serverSocket.accept())
      // TODO: (read) why is this an Option and a var? read about ServerSocket, Java class is there Scala equivalent?


      while (maybeClientSocket.nonEmpty) { // for comprehension here?
        // TODO: (style) instead of nonEmpty do pattern matching on Some(x) and None
        maybeClientSocket.foreach { clientSocket =>
          val bufferedSource = Source.fromInputStream(clientSocket.getInputStream())
          val userId = bufferedSource.bufferedReader().readLine()

          if (userId != null) {
            socketService.clientPool.put(userId.toLong, clientSocket)
            println(s"User connected: $userId (${socketService.clientPool.size} total)")
          }

          maybeClientSocket = Option(serverSocket.accept())
        }
      }
    }

    Await.result(Future.sequence(Seq(eventsAsync, clientsAsync)), Duration.Inf) // TODO: (read) why like this? Read about Future.sequence
  }

}
