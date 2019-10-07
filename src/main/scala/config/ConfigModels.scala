package config

// Product ADT

sealed trait Config
case class ApplicationConfig(name: String, eventPort: Int, clientPort: Int) extends Config
