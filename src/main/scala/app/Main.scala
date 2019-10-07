package app

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

import module.{ConfigModule, ClientModule, ServerModule, UserModule}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try

object Main extends App with ConfigModule with ServerModule with ClientModule {

  // TODO: (functionality) add optional port numbers in config so that they can be changed by inline args when starting the application
  // TODO: (style) fix config file not to be all over the place, use format like: app { bla = 2, bla2 = 4 }

  override def main(args: Array[String]): Unit = {

    Await.result(Future.sequence(Seq(eventService.eventsAsync, userService.clientsAsync)), Duration.Inf) // TODO: (read) why like this? Read about Future.sequence

  }

}
