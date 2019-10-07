package module

import service.SocketService

trait SocketModule { // i would have used macwire for DI but third party tools not allowed so using self type

  configModule: ConfigModule =>
    val socketService = new SocketService(configModule.applicationConfig)
}
