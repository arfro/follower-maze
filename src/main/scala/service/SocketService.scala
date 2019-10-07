package service

import java.net.{ServerSocket, Socket}

import config.ApplicationConfig

import scala.collection.concurrent.TrieMap

class SocketService(config: ApplicationConfig) { // easy to test - just change application.config
  val usersSocket = new ServerSocket(config.clientPort)
  val eventSocket = new ServerSocket(config.eventPort)

  val clientPool = new TrieMap[Long, Socket] // I like this choice. TrieMap is good for a concurrent environment
}
