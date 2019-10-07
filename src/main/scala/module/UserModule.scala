package module

import service.UserService

trait UserModule {

    socketModule: ServerModule => // depends on socket module
      val userService = new UserService(socketModule.serverService)


  }
