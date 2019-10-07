package service

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import app.Main.eventService
import config.AppConfig
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.collection.JavaConverters._

class EventService(socketService: ServerService) {

  var lastSeqNo = 0L // not vars!
  // does it have to be a TrieMap? Why?

  val messagesBySeqNo = new mutable.HashMap[Long, List[String]] // get rid of mutable?
  val followRegistry = new mutable.HashMap[Long, Set[Long]] // get rid of mutable?

  // ------------- 1  event source
  val eventsAsync = Future { // event module

    println(s"Listening for events on ${AppConfig.applicationConfig.eventPort}")
    // new socket for each event?
    val eventSocket = socketService.eventServerSocket.accept() // TODO: (read) ServerSocket.accept

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


}
