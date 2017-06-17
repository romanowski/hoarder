name := "hoarder"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "src" / "main" / "sbt_0.13"
(unmanagedSourceDirectories in Test) += baseDirectory.value / "src" / "test" / "sbt_0.13"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.11.4" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test

libraryDependencies +="junit" % "junit" % "4.11" % Test

libraryDependencies += "io.get-coursier" %% "coursier-cache" % "1.0.0-M15"