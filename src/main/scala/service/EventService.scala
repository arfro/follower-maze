package service

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import error.event.EventMessageDeliveryError
import model.alias.Aliases.{EventSequence, UserId}
import model.event.{Broadcast, Event, Follow, PrivateMessage, StatusUpdate, Unfollow}
import util.converters.{DeadLetterQueue, MessageConverter}

import scala.concurrent._
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try
import ExecutionContext.Implicits.global

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

  private def follow(follow: Follow): Either[EventMessageDeliveryError, Unit] = {
      val followersOfUser = followersRegistry.getOrElse(follow.toUser, Set.empty)
      val newFollowers = followersOfUser + follow.fromUser
      followersRegistry.put(follow.toUser, newFollowers)

      serverService.clientPool.get(follow.toUser) match {
        case Some(user) => Right(
          for {
            socket <- serverService.clientPool.get(follow.toUser) // TODO: repetition fix
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
            _ = writer.write(s"${follow.eventMessageRaw}\n") // add "toString" type class maybe to avoid hardcoding
            _ = writer.flush()
          } yield Unit
        )
        case None => {
          DeadLetterQueue.addToDeadLetterQueue(follow.eventMessageRaw)
          println(s"Cannot deliver message. User ${follow.toUser} not online. Added to DLQ: ${follow.eventMessageRaw}")
          Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${follow.eventMessageRaw}"))
        }
      }
    }

  private def unfollow(unfollow: Unfollow): Unit = {
      val followers = followersRegistry.getOrElse(unfollow.toUser, Set.empty)
      val newFollowers = followers - unfollow.fromUser
      followersRegistry.put(unfollow.toUser, newFollowers)
  }

  private def broadcast(broadcast: Broadcast): List[Either[EventMessageDeliveryError, Unit]] = {
    (for {
      (userId, socket) <- serverService.clientPool
      result = serverService.clientPool.get(userId) match {
        case Some(userId) => Right{val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          writer.write(s"${broadcast.eventMessageRaw}\n")
          writer.flush()}
        case None => {
          DeadLetterQueue.addToDeadLetterQueue(broadcast.eventMessageRaw)
          println(s"Cannot deliver message. User ${userId} not online. Added to DLQ: ${broadcast.eventMessageRaw}")
          Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${broadcast.eventMessageRaw}"))
        }
      }
    } yield result).toList
  }

  private def privateMessage(privateMessage: PrivateMessage): Either[EventMessageDeliveryError, Unit] = {
    serverService.clientPool.get(privateMessage.toUser) match {
      case Some(user) => Right(
        for {
          socket <- serverService.clientPool.get(privateMessage.toUser)
          writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          _ = writer.write(s"${privateMessage.eventMessageRaw}\n")
          _ = writer.flush()
        } yield Unit
      )
      case None => {
        DeadLetterQueue.addToDeadLetterQueue(privateMessage.eventMessageRaw)
        println(s"Cannot deliver message. User ${privateMessage.toUser} not online. Added to DLQ: ${privateMessage.eventMessageRaw}")
        Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${privateMessage.eventMessageRaw}"))
        // TODO: extract handling errors to a handler
      }
    }
  }

  private def statusUpdate(statusUpdate: StatusUpdate): List[Either[EventMessageDeliveryError, Unit]] = {
        for {
          followersOfFromUser <- followersRegistry.get(statusUpdate.fromUser).toList
          follower <- followersOfFromUser
          result = serverService.clientPool.get(follower) match {
            case Some(socket) => Right {
              val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
              writer.write(s"${statusUpdate.eventMessageRaw}\n")
              writer.flush()
            }
            case None => {
              DeadLetterQueue.addToDeadLetterQueue(statusUpdate.eventMessageRaw)
              println(s"Cannot deliver message. User ${follower} not online. Added to DLQ: ${statusUpdate.eventMessageRaw}")
              Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${statusUpdate.eventMessageRaw}"))
            }
          }
        } yield result
      }


  private def readFromBufferToHashMap(reader: BufferedReader): Unit =
      Iterator.continually(reader.readLine())
        .takeWhile(null != _) // remove null?
        .foreach { payload => {
          println(s"Message received: $payload") // should use logger instead of printing
          MessageConverter
            .convertEventMessageRawToEvent(payload) match {
                case Right(event) => messagesBySeqNo += event.sequence -> event
                case Left(errorMessage) => {
                  DeadLetterQueue.addToDeadLetterQueue(errorMessage.rawEventMessage)
                  println(s"Cannot deliver message. Message malformed. Added to DLQ: ${errorMessage.rawEventMessage}")
                  Left(EventMessageDeliveryError("Cannot deliver message. Message malformed.", s"${errorMessage.rawEventMessage}"))
                }
              }
            }
          }


  private def playEventsFromHashMapInSequence(hashMap: mutable.HashMap[EventSequence, Event]): Unit = {
    for (i <- 1 to hashMap.size) {
      hashMap.get(i) match {
        case Some(Follow(sequence, fromUser, toUser, raw)) =>
          follow(Follow(sequence, fromUser, toUser, raw))
        case Some(Unfollow(sequence, fromUser, toUser, raw)) =>
          unfollow(Unfollow(sequence, fromUser, toUser, raw))
        case Some(Broadcast(sequence, raw)) =>
          broadcast(Broadcast(sequence, raw))
        case Some(PrivateMessage(sequence, fromUser, toUser, raw)) =>
          privateMessage(PrivateMessage(sequence, fromUser, toUser, raw))
        case Some(StatusUpdate(sequence, fromUser, raw)) =>
          statusUpdate(StatusUpdate(sequence, fromUser, raw))
        case _ => Unit // TODO: (functionality) proper handling for the errors
      }
    }
  }




}