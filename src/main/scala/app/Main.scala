package app

import module.{ClientModule, ConfigModule, ServerModule}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Main extends App with ConfigModule with ServerModule with ClientModule {

  override def main(args: Array[String]): Unit = {

    /**
     * General improvements/notes:
     * - would add logging instead of printing
     * - would have the server run continuously
     * */
     Await.result(Future.sequence(
       Seq(
         eventService.eventsAsync(serverService),
         userService.clientsAsync(serverService))),
       Duration.Inf)

  }

}
