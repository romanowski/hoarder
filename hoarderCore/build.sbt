import HoarderSettings.autoimport._

name := "hoarderCore"

libraryDependencies ++= {
  if (isLegacySbt.value) Seq(
    "org.scala-sbt" % "persist" % sbtVersion.in(pluginCrossBuild).value,
    "org.scala-sbt" % "incremental-compiler" % sbtVersion.in(pluginCrossBuild).value
  ) else Seq(
    "org.scala-sbt" %% "zinc" % zincVersion
  )
}

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test
