package fixtures

import config.ApplicationConfig
import service.{EventService, ServerService}

trait DeadLetterQueueSpecFixture {

  val config = ApplicationConfig("name", 0, 0)
  val serverService = new ServerService(config)
  val eventService = new EventService(serverService)

}
