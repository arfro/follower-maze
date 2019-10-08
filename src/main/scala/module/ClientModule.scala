package module

import service.{EventService, UserClientsService}

trait ClientModule {

  serverModule: ServerModule =>
    val eventService = new EventService(serverModule.serverService)
    val userService  = new UserClientsService(serverModule.serverService)

}
