name := "maze"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  val typesafeConfigVersion = "1.3.4"
  //val slf4jVersion = "1.7.26"

  Seq(
    "com.typesafe" % "config" % typesafeConfigVersion,
   // "org.slf4j" % "slf4j-api" % slf4jVersion,
  //  "org.slf4j" % "slf4j-simple" % slf4jVersion
  )
}