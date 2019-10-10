package fixtures

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.{ServerSocket, Socket}

import config.ApplicationConfig
import model.alias.Aliases.UserId
import service.{EventService, ServerService, UserClientsService}

import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

import ExecutionContext.Implicits.global

trait DeadLetterQueueSpecFixture extends MockitoSugar {

  // mocks - config
  private val config = mock[ApplicationConfig]

  // event related mocks and stubbing enablers
  private val eventServerSocketMock = mock[ServerSocket]
  private val eventSocketMock = mock[Socket]
  private val outputStreamEvent = new ByteArrayOutputStream()
  Mockito.when(eventServerSocketMock.getLocalPort).thenReturn(24222)
  Mockito.when(eventServerSocketMock.accept()).thenReturn(eventSocketMock)
  Mockito.when(eventSocketMock.getLocalPort).thenReturn(24222)
  Mockito.when(eventSocketMock.getOutputStream()).thenReturn(outputStreamEvent)

  // client related mocks and stubbing enablers
  private val clientServerSocketMock = mock[ServerSocket]
  private val clientSocketMock = mock[Socket]
  private val outputStreamClient = new ByteArrayOutputStream()
  Mockito.when(clientServerSocketMock.getLocalPort).thenReturn(24224)
  Mockito.when(clientServerSocketMock.accept()).thenReturn(clientSocketMock)
  Mockito.when(clientSocketMock.getLocalPort).thenReturn(24224)
  Mockito.when(clientSocketMock.getOutputStream).thenReturn(outputStreamClient)


  private val serverService = new ServerService(config) {
    override val eventServerSocket: ServerSocket = eventServerSocketMock
    override val usersServerSocket: ServerSocket = clientServerSocketMock
    override val clientPool: TrieMap[UserId, Socket] = new TrieMap()
  }
  private val eventService = new EventService(serverService)
  private val userService = new UserClientsService(serverService)

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
