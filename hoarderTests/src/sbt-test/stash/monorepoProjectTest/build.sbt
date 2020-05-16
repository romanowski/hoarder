lazy val commonProject = module("common")
lazy val fooProject = module("foo").dependsOn(commonProject)
lazy val barProject = module("bar").dependsOn(commonProject)

scalaVersion := "2.13.2"

val commonSettings = Seq(
  scalaVersion in ThisProject := "2.13.2",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.1.1"
  )
)

def module(name: String): Project =
  Project(id = name, base = file(s"modules/$name")).settings(commonSettings)
