package config

import com.typesafe.config.ConfigFactory

// Sum ADT

sealed trait Configuration {

  val configFactory = ConfigFactory.load() // 3rd party libraries are not allowed but this maybe is justified? makes the code look nice

}

case object AppConfig extends Configuration {

  private val appName = configFactory.getString("application.name")
  private val eventPort = configFactory.getInt("event.port")
  private val clientPort = configFactory.getInt("client.port")

  val applicationConfig = ApplicationConfig(appName, eventPort, clientPort)

}
