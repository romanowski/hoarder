import HoarderSettings.autoimport._


name := "hoarderCore"

libraryDependencies ++= {
  sbtPrefix.value match {
    case "0.13" => Nil
    case "1.0" =>
      Seq(
        "org.scala-sbt" %% "zinc" % "1.0.0-X20"
      )
  }


}