package service

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import app.Main.eventService
import config.AppConfig
import model.alias.Aliases.{EventSequence, UserId}
import model.event.{Broadcast, Event, EventMessageRaw, Follow, PrivateMessage, StatusUpdate, Unfollow}
import util.converters.MessageConverter

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.collection.JavaConverters._

class EventService(socketService: ServerService) {

  var lastSeqNo = 0L // not vars!

  val messagesBySeqNo = new mutable.HashMap[Long, List[String]] // get rid of mutable?
  val messagesBySeqNo2 = new mutable.HashMap[EventSequence, Event] // get rid of mutable?
  val followRegistry = new mutable.HashMap[Long, Set[Long]] // get rid of mutable?

  val eventsAsync = Future { // event module

    // TODO: (style) hook up config service here
    println(s"Listening for events on ${AppConfig.applicationConfig.eventPort}")

    val eventSocket = socketService.eventServerSocket.accept()

    // have the socket accept connections
    // read lines from socket
    // get the message sequence and split on it
    // create a map of sequence nr -> message
    // for each message act on it
    // once done, close the reader and the socket

//    val f = for {
//      reader <- Try(new BufferedReader(new InputStreamReader(eventSocket.getInputStream())))
//      _ = println(reader)
//      iterator <- reader.lines().iterator().asScala
//
//    } yield reader
//
//     println(f)


    // remove exceptions throwing maybe use either?
    Try { // TODO: (functionality) handling input in Scala correctly also read about BufferedReader - is there a better way to read from Socket?
      // TODO: (read) BufferedReader
      val reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()))

     Iterator.continually(reader.readLine()).takeWhile(null != _).foreach{ payload => {
        MessageConverter.convertEventMessageRawToEvent(EventMessageRaw(payload)) // TODO: (style) does it make sense to have this EventMessagrRaw at all?
           .map(event => eventService.messagesBySeqNo2 += event.sequence -> event)
         println(eventService.messagesBySeqNo2 + "\n\n")
       }
     }

      for (i <- 1 to eventService.messagesBySeqNo2.size) {
        eventService.messagesBySeqNo2.get(i) match {
          case Some(Follow(sequence, fromUser, toUser)) => follow(sequence, fromUser, toUser)
          case Some(Unfollow(sequence, fromUser, toUser)) => unfollow(sequence, fromUser, toUser)
          case Some(Broadcast(sequence)) => broadcast(sequence)
          case Some(PrivateMessage(sequence, fromUser, toUser)) => privateMessage(sequence, fromUser, toUser)
          case Some(StatusUpdate(sequence, fromUser)) => statusUpdate(sequence, fromUser)
          case Some(_) => ""
          case None => ""
        }
      }


      Try {
        reader.lines().iterator().asScala.foreach { payload =>
          println(s"Message received: $payload")
          val msg = MessageConverter.convertEventMessageRawToEvent(EventMessageRaw(payload))

          val message = payload.split("\\|").toList

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

  private def follow(sequence: EventSequence, fromUser: UserId, toUser: UserId) = println(s"FOLLOW! $sequence\n\n")
  private def unfollow(sequence: EventSequence, fromUser: UserId, toUser: UserId) = println(s"UNFOLLOW! $sequence\n\n")
  private def broadcast(sequence: EventSequence) = println(s"BROADCST! $sequence\n\n")
  private def privateMessage(sequence: EventSequence, fromUser: UserId, toUser: UserId) = println(s"PRIV! $sequence\n\n")
  private def statusUpdate(sequence: EventSequence, fromUser: UserId) = println(s"STATUS! $sequence\n\n")

}
