package module

import service.{EventService, UserService}

trait ClientModule {

  socketModule: ServerModule =>
    val eventService = new EventService(socketModule.serverService)
    val userService  = new UserService(socketModule.serverService)

}
