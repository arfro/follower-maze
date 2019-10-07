package service

import java.net.{ServerSocket, Socket}

import config.ApplicationConfig

import scala.collection.concurrent.TrieMap

class ServerService(config: ApplicationConfig) { // easy to test - just change config

  val usersServerSocket = new ServerSocket(config.clientPort)
  val eventServerSocket = new ServerSocket(config.eventPort)

  val clientPool = new TrieMap[Long, Socket] // I like this choice. TrieMap is good for a concurrent environment
}
