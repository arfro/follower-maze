package module

import service.ConfigService

trait ConfigModule {

  lazy val configService = new ConfigService

}