name := "maze"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  val typesafeConfigVersion = "1.3.4"
  val scalaTestVersion = "3.0.8"
  //val slf4jVersion = "1.7.26"

  Seq(
    "com.typesafe" % "config" % typesafeConfigVersion,
    "org.scalactic" %% "scalactic" % scalaTestVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
   // "org.slf4j" % "slf4j-api" % slf4jVersion,
  //  "org.slf4j" % "slf4j-simple" % slf4jVersion
  )
}