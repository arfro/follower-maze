package module

import service.ServerService

trait ServerModule { // i would have used macwire for DI but third party tools not allowed so using self type

  configModule: ConfigModule =>
    val serverService = new ServerService(configModule.configService.applicationConfig)
}
