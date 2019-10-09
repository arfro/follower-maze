package service

import java.net.{ServerSocket, Socket}

import config.AppConfig

import scala.concurrent.Future
import scala.io.Source
import scala.concurrent._
import ExecutionContext.Implicits.global

class UserClientsService(serverService: ServerService) {

  def clientsAsync(serverService: ServerService) = Future {

    println(s"Listening for client requests on ${AppConfig.applicationConfig.usersClientPort}")
    val usersSocket = serverService.usersServerSocket.accept()
    val bufferedSource = Source.fromInputStream(usersSocket.getInputStream())

    Iterator.continually(bufferedSource.bufferedReader().readLine())
      .takeWhile(null != _)
      .foreach( userId => {
        serverService.clientPool.put(userId.toLong, usersSocket)
        println(s"User connected: $userId (${serverService.clientPool.size} total)")
      }
    )
  }

}
