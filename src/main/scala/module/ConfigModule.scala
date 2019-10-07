package module

import config.AppConfig

trait ConfigModule {

  // TODO: (style) take this out to configService
  lazy val applicationConfig = AppConfig.applicationConfig // only call when needed

}