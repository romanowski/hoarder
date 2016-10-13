name := "cacher-1.0"

version := "1.0"

scalaVersion := "2.11.8"

//sbtPlugin := true

organization := "org.romanowski"

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "zinc" % "1.0-cached-SNAPSHOT",
  "org.scala-sbt" %% "main" % "1.0.0-cached-SNAPSHOT",
  "org.scala-sbt" % "sbt" % "1.0.0-cached-SNAPSHOT",
  "org.scala-sbt" %% "zinc" % "1.0-cached-SNAPSHOT" % "test->test"//, // classifier("tests"),
 // "org.scala-sbt" %% "zinc-testing" % "1.0-cached-SNAPSHOT" % "test->test"
)

