package fixtures

import java.io.{BufferedWriter, ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStreamWriter}
import java.net.{ServerSocket, Socket}

import app.Main.{eventService, serverService, userService}
import config.ApplicationConfig
import model.alias.Aliases.UserId
import org.mockito.Mockito
import service.{EventService, ServerService}
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait DeadLetterQueueSpecFixture extends MockitoSugar {

  val config = mock[ApplicationConfig]

  val eventServerSocket = mock[ServerSocket]
  val eventSocket = mock[Socket]

  val clientServerSocket = mock[ServerSocket]
  val clientSocket = mock[Socket]

  val serverService = new ServerService(config) {
    override val eventServerSocket: ServerSocket = eventServerSocket
    override val usersServerSocket: ServerSocket = clientServerSocket
    override val clientPool: TrieMap[UserId, Socket] = new TrieMap()
  }

  val inputStreamEvent = new ByteArrayInputStream("32|f|23|23\n".getBytes())
  val outputStreamEvent = new ByteArrayOutputStream()

  val inputStreamClient = new ByteArrayInputStream("32\n".getBytes())
  val outputStreamClient = new ByteArrayOutputStream()

  // event
  Mockito.when(eventServerSocket.getLocalPort).thenReturn(24)
  Mockito.when(eventServerSocket.accept()).thenReturn(eventSocket)
  Mockito.when(eventSocket.getLocalPort).thenReturn(24)
  Mockito.when(eventSocket.getInputStream()).thenReturn(inputStreamEvent)
  Mockito.when(eventSocket.getOutputStream()).thenReturn(outputStreamEvent)

  // client
  Mockito.when(clientServerSocket.accept()).thenReturn(clientSocket)
  Mockito.when(clientSocket.getInputStream).thenReturn(inputStreamClient)
  Mockito.when(clientSocket.getOutputStream).thenReturn(outputStreamClient)

  Mockito.when(config.eventPort).thenReturn(99)
  Mockito.when(config.usersClientPort).thenReturn(999)

  val eventService = new EventService(serverService)

  Await.result(Future.sequence(
    Seq(
      eventService.eventsAsync(serverService),
      userService.clientsAsync(serverService))),
    Duration.Inf)

  val writer = new BufferedWriter(new OutputStreamWriter(eventSocket.getOutputStream()))
  writer.write(s"sasdasd\n")
  writer.flush()

  println()

}
