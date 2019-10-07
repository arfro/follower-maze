package model.event

import model.alias.Aliases.{EventSequence, UserId}


case class EventMessageRaw(payload: String)

// Product ADT

trait Event
case class Broadcast(sequence: EventSequence) extends Event
case class Follow(sequence: EventSequence, fromUser: UserId, toUser: UserId) extends Event
case class Unfollow(sequence: EventSequence, fromUser: UserId, toUser: UserId) extends Event
case class PrivateMessage(sequence: EventSequence, fromUser: UserId, toUser: UserId) extends Event
case class StatusUpdate(sequence: EventSequence, fromUser: UserId) extends Event
