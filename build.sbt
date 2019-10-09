name := "maze"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  val typesafeConfigVersion = "1.3.4"
  val scalaTestVersion = "3.0.8"
  val mockitoVersion = "3.0.0"

  Seq(
    "com.typesafe" % "config" % typesafeConfigVersion,
    "org.scalactic" %% "scalactic" % scalaTestVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.mockito" % "mockito-core" % mockitoVersion % "test"
  )
}