package config

import com.typesafe.config.ConfigFactory

sealed trait Configuration {

  val configFactory = ConfigFactory.load()

}

case object AppConfig extends Configuration {

  private val appName = configFactory.getString("application.name")
  private val eventPort = configFactory.getInt("event.port")
  private val clientPort = configFactory.getInt("client.port")

  val applicationConfig = ApplicationConfig(appName, eventPort, clientPort)

}
