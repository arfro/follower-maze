package module

import service.EventService

trait EventModule {

  socketModule: SocketModule => // depends on socket module
      val eventService = new EventService(socketModule.socketService)


}
