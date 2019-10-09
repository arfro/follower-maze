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

  val eventServerSocketMock = mock[ServerSocket]
  val eventSocketMock = mock[Socket]

  val clientServerSocketMock = mock[ServerSocket]
  val clientSocketMock = mock[Socket]

  val outputStreamEvent = new ByteArrayOutputStream()

  val outputStreamClient = new ByteArrayOutputStream()

  //event
  Mockito.when(eventServerSocketMock.getLocalPort).thenReturn(24222)
  Mockito.when(eventServerSocketMock.accept()).thenReturn(eventSocketMock)
  Mockito.when(eventSocketMock.getLocalPort).thenReturn(24222)
  Mockito.when(eventSocketMock.getOutputStream()).thenReturn(outputStreamEvent)

  // client
  Mockito.when(clientServerSocketMock.getLocalPort).thenReturn(24224)
  Mockito.when(clientServerSocketMock.accept()).thenReturn(clientSocketMock)
  Mockito.when(clientSocketMock.getLocalPort).thenReturn(24224)
  Mockito.when(clientSocketMock.getOutputStream).thenReturn(outputStreamClient)



  val serverService = new ServerService(config) {
    override val eventServerSocket: ServerSocket = eventServerSocketMock
    override val usersServerSocket: ServerSocket = clientServerSocketMock
    override val clientPool: TrieMap[UserId, Socket] = new TrieMap()
  }

  val eventService = new EventService(serverService)

  def run() = Await.result(Future.sequence(
    Seq(
      userService.clientsAsync(serverService),
      eventService.eventsAsync(serverService))),
    Duration.Inf)

  def sendMessageToEventSocket(message: String): Unit = {
    val inputStreamEvent = new ByteArrayInputStream(s"$message\n".getBytes())
    Mockito.when(eventSocketMock.getInputStream).thenReturn(inputStreamEvent)
  }

  def sendMessageToClientSocket(message: String): Unit = {
    val inputStreamClient = new ByteArrayInputStream(s"$message\n".getBytes())
    Mockito.when(clientSocketMock.getInputStream).thenReturn(inputStreamClient)
  }


}
