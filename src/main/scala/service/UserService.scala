package service

import config.AppConfig

import scala.concurrent.Future
import scala.io.Source
import scala.concurrent._
import ExecutionContext.Implicits.global

class UserService(socketService: ServerService) {

  // ------------- N user-clients
  val clientsAsync = Future {

    println(s"Listening for client requests on ${AppConfig.applicationConfig.clientPort}") // logger instead of print
    val serverSocket = socketService.usersServerSocket
    println(s"server socket: $serverSocket")
    var maybeClientSocket = Option(serverSocket.accept())
    // TODO: (read) why is this an Option and a var? read about ServerSocket, Java class is there Scala equivalent?


    while (maybeClientSocket.nonEmpty) { // for comprehension here?
      // TODO: (style) instead of nonEmpty do pattern matching on Some(x) and None
      maybeClientSocket.foreach { clientSocket =>
        val bufferedSource = Source.fromInputStream(clientSocket.getInputStream())
        val userId = bufferedSource.bufferedReader().readLine()

        if (userId != null) {
          socketService.clientPool.put(userId.toLong, clientSocket)
          println(s"User connected: $userId (${socketService.clientPool.size} total)")
        }

        maybeClientSocket = Option(serverSocket.accept())
      }
    }
  }

}
