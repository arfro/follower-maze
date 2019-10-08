package module

import service.ConfigService

trait ConfigModule {


  // TODO: (functionality) add optional port numbers in config so that they can be changed by inline args when starting the application
  // TODO: (style) fix config file not to be all over the place, use format like: app { bla = 2, bla2 = 4 }

  lazy val configService = new ConfigService

}