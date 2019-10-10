package config

sealed trait Config
case class ApplicationConfig(name: String, eventPort: Int, usersClientPort: Int) extends Config
