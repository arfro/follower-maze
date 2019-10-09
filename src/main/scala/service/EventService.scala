package service

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import model.alias.Aliases.{EventSequence, UserId}
import model.error.EventMessageDeliveryError
import model.event.{Broadcast, Event, Follow, PrivateMessage, StatusUpdate, Unfollow}
import util.DeadLetterQueue
import util.converters.MessageConverter

import scala.concurrent._
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try
import ExecutionContext.Implicits.global

class EventService(serverService: ServerService) {

  /***
   * Improvements:
   * - I would prefer to use immutable data structures
   * */
  val followersRegistry = new mutable.HashMap[UserId, Set[UserId]]
  val messagesBySeqNo = new mutable.HashMap[EventSequence, Event]

  def eventsAsync(serverService: ServerService) = Future {
    println(s"Listening for events on ${serverService.eventServerSocket.getLocalPort}")
      val eventSocket = serverService.eventServerSocket.accept()

      for {
        reader <- Try(new BufferedReader(new InputStreamReader(eventSocket.getInputStream())))
        _ = readFromBufferToHashMap(reader)
        _ = playEventsFromHashMapInSequence(messagesBySeqNo)
        _ = reader.close()
        _ = eventSocket.close()
      } yield Unit
  }

  private def follow(follow: Follow): Either[EventMessageDeliveryError, Unit] = {
      val followersOfUser = followersRegistry.getOrElse(follow.toUser, Set.empty)
      val newFollowers = followersOfUser + follow.fromUser
      followersRegistry.put(follow.toUser, newFollowers)

      serverService.clientPool.get(follow.toUser) match {
        case Some(socket) => Right {
            val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
            writer.write(s"${follow.eventMessageRaw}\n")
            writer.flush()
          }
        case None => {
            DeadLetterQueue.addToDeadLetterQueue(follow.eventMessageRaw)
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
        case Some(userId) => Right{
          val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
          writer.write(s"${broadcast.eventMessageRaw}\n")
          writer.flush()
        }
        case None => {
          DeadLetterQueue.addToDeadLetterQueue(broadcast.eventMessageRaw)
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
        Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${privateMessage.eventMessageRaw}"))
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
              Left(EventMessageDeliveryError("Cannot deliver message. User not online", s"${statusUpdate.eventMessageRaw}"))
            }
          }
        } yield result
      }


  /**
   * Improvements:
   * - not a big fan of using null even in "safe" cases as this one, I would probably seek another way, wouldn't ship like this
   */
  private def readFromBufferToHashMap(reader: BufferedReader): Unit =
      Iterator.continually(reader.readLine())
        .takeWhile(null != _)
        .foreach { payload => {
          println(s"Message received: $payload") // should use logger instead of printing
          MessageConverter
            .convertEventMessageRawToEvent(payload) match {
                case Right(event) => messagesBySeqNo += event.sequence -> event
                case Left(errorMessage) => {
                  DeadLetterQueue.addToDeadLetterQueue(errorMessage.rawEventMessage)
                  Left(EventMessageDeliveryError("Cannot deliver message. Message malformed.", s"${errorMessage.rawEventMessage}"))
                }
              }
            }
          }


  /**
   * Improvements:
   * - I would add proper error handling here so that the return type is an Either rather than Unit. The infrastructure
   * is pretty much ready in the called functions
   * */
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
        case _ => Unit
      }
    }
  }




}