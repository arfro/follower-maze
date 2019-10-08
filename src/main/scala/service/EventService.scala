package service

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import model.alias.Aliases.{EventSequence, UserId}
import model.event.{Broadcast, Event, EventMessageRaw, Follow, PrivateMessage, StatusUpdate, Unfollow}
import util.converters.MessageConverter

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try

class EventService(serverService: ServerService) {

  val followersRegistry = new mutable.HashMap[UserId, Set[UserId]] // get rid of mutable?
  val messagesBySeqNo = new mutable.HashMap[EventSequence, Event] // get rid of mutable?

  val eventsAsync = Future {
    println(s"Listening for events on ${serverService.eventServerSocket.getLocalPort}")
    val eventSocket = serverService.eventServerSocket.accept()

      for {
        reader <- Try(new BufferedReader(new InputStreamReader(eventSocket.getInputStream())))
        _ = readFromBufferToHashMap(reader)
        _ = playEventsFromHashMapInSequence(messagesBySeqNo)
        _ = reader.close()
        _ = serverService.eventServerSocket.close()
      } yield Unit
  }

  private def follow(sequence: EventSequence, fromUser: UserId, toUser: UserId, followers: mutable.HashMap[UserId, Set[UserId]]): Unit = {
      val followersOfUser = followersRegistry.getOrElse(toUser, Set.empty)
      val newFollowers = followersOfUser + fromUser
      followersRegistry.put(toUser, newFollowers)

      for {
        socket <- serverService.clientPool.get(toUser)
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        _ = writer.write(s"$sequence|F|$fromUser|$toUser\n") // add "toString" type class maybe to avoid hardcoding
        _ = writer.flush()
      } yield Unit
    }

  private def unfollow(sequence: EventSequence, fromUser: UserId, toUser: UserId) = {
      val followers = followersRegistry.getOrElse(toUser, Set.empty)
      val newFollowers = followers - fromUser
      followersRegistry.put(toUser, newFollowers)
  }

  private def broadcast(sequence: EventSequence): Unit = {
    for {
      socket <- serverService.clientPool.values
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      _ = writer.write(s"$sequence|B\n")
      _ = writer.flush()
    } yield Unit
  }

  private def privateMessage(sequence: EventSequence, fromUser: UserId, toUser: UserId): Unit = {
    for {
      socket <- serverService.clientPool.get(toUser)
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      _ = writer.write(s"$sequence|P|$fromUser|$toUser\n")
      _ = writer.flush()
    } yield Unit
  }

  private def statusUpdate(sequence: EventSequence, fromUser: UserId, followersRegistry: mutable.HashMap[UserId, Set[UserId]]): Unit = {
    followersRegistry.get(fromUser).foreach { followersOfFromUser =>
        for {
          follower <- followersOfFromUser
          socket <- serverService.clientPool.get(follower)
          writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          _ = writer.write(s"$sequence|S|$fromUser\n")
          _ = writer.flush()
        } yield Unit
      }
  }

  private def readFromBufferToHashMap(reader: BufferedReader): Unit =
      Iterator.continually(reader.readLine())
        .takeWhile(null != _) // remove null?
        .foreach { payload => {
          println(s"Message received: $payload") // should use logger instead of printing
          MessageConverter
            .convertEventMessageRawToEvent(EventMessageRaw(payload)) // TODO: (style) does it make sense to have this EventMessagrRaw at all?
            .map(event => messagesBySeqNo += event.sequence -> event)
          }
        }

  private def playEventsFromHashMapInSequence(hashMap: mutable.HashMap[EventSequence, Event]): Unit = {
    for (i <- 1 to hashMap.size) {
      hashMap.get(i) match {
        case Some(Follow(sequence, fromUser, toUser)) => follow(sequence, fromUser, toUser, followersRegistry)
        case Some(Unfollow(sequence, fromUser, toUser)) => unfollow(sequence, fromUser, toUser)
        case Some(Broadcast(sequence)) => broadcast(sequence)
        case Some(PrivateMessage(sequence, fromUser, toUser)) => privateMessage(sequence, fromUser, toUser)
        case Some(StatusUpdate(sequence, fromUser)) => statusUpdate(sequence, fromUser, followersRegistry)
        case _ => Unit // TODO: (functionality) proper handling for the errors
      }
    }
  }



}