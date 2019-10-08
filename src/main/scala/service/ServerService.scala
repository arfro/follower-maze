package service

import java.net.{ServerSocket, Socket}

import config.ApplicationConfig
import model.alias.Aliases.UserId

import scala.collection.concurrent.TrieMap

class ServerService(config: ApplicationConfig) {

  val usersServerSocket = new ServerSocket(config.usersClientPort)
  val eventServerSocket = new ServerSocket(config.eventPort)

  val clientPool = new TrieMap[UserId, Socket]
}
