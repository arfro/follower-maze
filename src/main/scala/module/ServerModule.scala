package module

import service.ServerService

trait ServerModule {
  /***
   * Improvements:
   * - As no third party library are allowed I am using self types. I would usually incline towards DI tools such as Macwire
   * */
  configModule: ConfigModule =>
    val serverService = new ServerService(configModule.configService.applicationConfig)
}
