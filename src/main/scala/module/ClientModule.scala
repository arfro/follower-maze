package module

import service.{EventService, UserClientsService}

trait ClientModule {

  socketModule: ServerModule =>
    val eventService = new EventService(socketModule.serverService)
    val userService  = new UserClientsService(socketModule.serverService)

}
