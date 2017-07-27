import HoarderSettings.autoimport._

name := "hoarderCore"

libraryDependencies += {
  sbtPrefix.value match {
    case "0.13" =>
       "com.typesafe.sbt" % "incremental-compiler" % sbtVersion.in(pluginCrossBuild).value
    case "1.0" =>
        "org.scala-sbt" %% "zinc" % "1.0.0-X20"
  }


}

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test
