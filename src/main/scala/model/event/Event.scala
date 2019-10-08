package model.event

import model.alias.Aliases.{EventMessageRaw, EventSequence, UserId}

// Product ADT

trait Event {
  val sequence: EventSequence
  val eventMessageRaw: EventMessageRaw
}
case class Broadcast(sequence: EventSequence, eventMessageRaw: EventMessageRaw) extends Event
case class Follow(sequence: EventSequence, fromUser: UserId, toUser: UserId, eventMessageRaw: EventMessageRaw) extends Event
case class Unfollow(sequence: EventSequence, fromUser: UserId, toUser: UserId, eventMessageRaw: EventMessageRaw) extends Event
case class PrivateMessage(sequence: EventSequence, fromUser: UserId, toUser: UserId, eventMessageRaw: EventMessageRaw) extends Event
case class StatusUpdate(sequence: EventSequence, fromUser: UserId, eventMessageRaw: EventMessageRaw) extends Event
