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

  val inputStreamEvent = new ByteArrayInputStream("32|x|23|23\n".getBytes())
  val outputStreamEvent = new ByteArrayOutputStream()

  val inputStreamClient = new ByteArrayInputStream("32\n".getBytes())
  val outputStreamClient = new ByteArrayOutputStream()

  Mockito.when(config.eventPort).thenReturn(24222)
  Mockito.when(config.usersClientPort).thenReturn(24223)

  //event
  Mockito.when(eventServerSocketMock.getLocalPort).thenReturn(24222)
  Mockito.when(eventServerSocketMock.accept()).thenReturn(eventSocketMock)
  Mockito.when(eventSocketMock.getLocalPort).thenReturn(24223)
  Mockito.when(eventSocketMock.getInputStream()).thenReturn(inputStreamEvent)
  Mockito.when(eventSocketMock.getOutputStream()).thenReturn(outputStreamEvent)

  // client
  Mockito.when(clientServerSocketMock.accept()).thenReturn(clientSocketMock)
  Mockito.when(clientSocketMock.getInputStream).thenReturn(inputStreamClient)
  Mockito.when(clientSocketMock.getOutputStream).thenReturn(outputStreamClient)



  val serverService = new ServerService(config) {
    override val eventServerSocket: ServerSocket = eventServerSocketMock
    override val usersServerSocket: ServerSocket = clientServerSocketMock
    override val clientPool: TrieMap[UserId, Socket] = new TrieMap()
  }


  val eventService = new EventService(serverService)

  println(eventService)
  println(serverService.eventServerSocket.getLocalPort)


  Await.result(Future.sequence(
    Seq(
      eventService.eventsAsync(serverService),
      userService.clientsAsync(serverService))),
    Duration.Inf)

  val writer = new BufferedWriter(new OutputStreamWriter(eventSocketMock.getOutputStream()))
  writer.write(s"sasdasd\n")
  writer.flush()

  def sendMessageToEventSocket(message: String): Unit ={

  }

  def sendMessageToClientSocket(message: String) = {

  }

}
