package app

import module.{ClientModule, ConfigModule, ServerModule}
import util.converters.DeadLetterQueue

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Main extends App with ConfigModule with ServerModule with ClientModule {

  override def main(args: Array[String]): Unit = {

    Await.result(Future.sequence(Seq(eventService.eventsAsync, userService.clientsAsync)), Duration.Inf)

    println(s"Dead letter queue: ${DeadLetterQueue.deadLetterQueue}")

  }

}
