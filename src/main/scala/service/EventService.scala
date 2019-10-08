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
import scala.concurrent.Future
import scala.util.Try
import scala.collection.JavaConverters._

class EventService(serverService: ServerService) {

 // var lastSeqNo = 0L // not vars!

  // val messagesBySeqNo = new mutable.HashMap[Long, List[String]] // get rid of mutable?

  val followRegistry = new mutable.HashMap[UserId, Set[UserId]] // get rid of mutable?
  val messagesBySeqNo2 = new mutable.HashMap[EventSequence, Event] // get rid of mutable?

  val eventsAsync = Future {

    println(s"Listening for events on ${serverService.eventServerSocket.getLocalPort}")

    val eventSocket = serverService.eventServerSocket.accept()

      for { // would have use cats for IO
        reader <- Try(new BufferedReader(new InputStreamReader(eventSocket.getInputStream())))
        _ = readFromBufferToHashMap(reader)
        _ = playEventsFromHashMapInSequence(messagesBySeqNo2)
        _ = reader.close()
        _ = serverService.eventServerSocket.close()
      } yield Unit

//    Try {
//
//      Try {
//        reader.lines().iterator().asScala.foreach { payload =>
//          println(s"Message received: $payload")
//
//          val message = payload.split("\\|").toList
//
//          eventService.messagesBySeqNo += message(0).toLong -> message
//
//          while (eventService.messagesBySeqNo.get(lastSeqNo + 1L).isDefined) {
//            val nextMessage = eventService.messagesBySeqNo(lastSeqNo + 1)
//
//            eventService.messagesBySeqNo -= lastSeqNo + 1L
//
//            val nextPayload = nextMessage.mkString("|")
//            val seqNo = nextMessage(0).toLong
//            val kind = nextMessage(1)
//
//            kind match {
//              case "F" => // ADT instead of a string
//                val fromUserId = nextMessage(2).toLong
//                val toUserId = nextMessage(3).toLong
//                val followers = eventService.followRegistry.getOrElse(toUserId, Set.empty)
//                val newFollowers = followers + fromUserId
//
//                eventService.followRegistry.put(toUserId, newFollowers)
//
//                serverService.clientPool.get(toUserId).foreach { socket => //
//                  val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
//                  writer.write(s"$nextPayload\n")
//                  writer.flush()
//                }
//
//              case "U" =>
//                val fromUserId = nextMessage(2).toLong
//                val toUserId = nextMessage(3).toLong
//                val followers = eventService.followRegistry.getOrElse(toUserId, Set.empty)
//                val newFollowers = followers - fromUserId
//
//                eventService.followRegistry.put(toUserId, newFollowers)
//
//              case "P" =>
//                serverService.clientPool.get(toUserId).foreach { socket =>
//                  val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
//                  writer.write(s"$nextPayload\n")
//                  writer.flush()
//                }
//
//              case "B" =>
//                serverService.clientPool.values.foreach { socket =>
//                  val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
//                  writer.write(s"$nextPayload\n")
//                  writer.flush()
//                }
//
//              case "S" =>
//                val fromUserId = nextMessage(2).toLong
//                val followers = eventService.followRegistry.getOrElse(fromUserId, Set.empty)
//
//                followers.foreach { follower =>
//                  serverService.clientPool.get(follower).foreach { socket =>
//                    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
//                    writer.write(s"$nextPayload\n")
//                    writer.flush()
//                  }
//                }
//            }
//
//            lastSeqNo = seqNo // update as you copy!
//          }
//        }
//      }
//      if (reader != null) reader.close() // NPE handling here
//    }
//    if (eventSocket != null) eventSocket.close() // NPE handling here
  }

  private def follow(sequence: EventSequence, fromUser: UserId, toUser: UserId, followers: mutable.HashMap[UserId, Set[UserId]]): Unit =
    {
      val followersOfUser = eventService.followRegistry.getOrElse(toUser, Set.empty)
      println(s"$fromUser want to follow $toUser: seq -$sequence\n")
      val newFollowers = followersOfUser + fromUser
      eventService.followRegistry.put(toUser, newFollowers)
      println(s"\nfollowers of $toUser: ${eventService.followRegistry.get(toUser)}\n")
      serverService.clientPool.get(toUser).foreach { socket =>
          val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          writer.write(s"$sequence|F|$fromUser|$toUser\n") // TODO: add a toString for case classes
          writer.flush()
      }
    }
  private def unfollow(sequence: EventSequence, fromUser: UserId, toUser: UserId) = {
    println(s"$fromUser want to unfollow $toUser: seq -$sequence\n")
    val followers = eventService.followRegistry.getOrElse(toUser, Set.empty)
    val newFollowers = followers - fromUser
    eventService.followRegistry.put(toUser, newFollowers)
    println(s"\nfollowers of $toUser: ${eventService.followRegistry.get(toUser)}\n")
  }
  private def broadcast(sequence: EventSequence): Unit = {
    println(s"broadcasting! seq -$sequence\n")
    serverService.clientPool.values.foreach { socket =>
        val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        writer.write(s"$sequence|B\n")
        writer.flush()
      }
  }
  private def privateMessage(sequence: EventSequence, fromUser: UserId, toUser: UserId): Unit = {
    println(s"priv! seq -$sequence\n")
    serverService.clientPool.get(toUser).foreach { socket =>
      val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      writer.write(s"$sequence|P|$fromUser|$toUser\n")
      writer.flush()
    }
  }
  private def statusUpdate(sequence: EventSequence, fromUser: UserId, followers: mutable.HashMap[UserId, Set[UserId]]): Unit = {
    println(s"STATUS! $sequence\n\n")
    followers.foreach { follower =>
          serverService.clientPool.get(follower._1).foreach { socket =>
          val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          writer.write(s"$sequence|S|$fromUser\n")
          writer.flush()
        }
      }
  }


  private def readFromBufferToHashMap(reader: BufferedReader): Unit =
      Iterator.continually(reader.readLine())
        .takeWhile(null != _) // remove null?
        .foreach { payload => {
          println(s"Message received: $payload") // should use logger instead of printing
          MessageConverter
            .convertEventMessageRawToEvent(EventMessageRaw(payload)) // TODO: (style) does it make sense to have this EventMessagrRaw at all?
            .map(event => eventService.messagesBySeqNo2 += event.sequence -> event)
          }
        }

  private def playEventsFromHashMapInSequence(hashMap: mutable.HashMap[EventSequence, Event]): Unit = {
    for (i <- 1 to hashMap.size) {
      hashMap.get(i) match {
        case Some(Follow(sequence, fromUser, toUser)) => follow(sequence, fromUser, toUser, followRegistry)
        case Some(Unfollow(sequence, fromUser, toUser)) => unfollow(sequence, fromUser, toUser)
        case Some(Broadcast(sequence)) => broadcast(sequence)
        case Some(PrivateMessage(sequence, fromUser, toUser)) => privateMessage(sequence, fromUser, toUser)
        case Some(StatusUpdate(sequence, fromUser)) => statusUpdate(sequence, fromUser, followRegistry)
        case Some(_) => ""
        case None => ""
      }
    }
  }



}