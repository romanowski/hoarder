import sbt.dsl._

def settings = org.romanowski.hoarder.tests.PluginTests.testRecompilation

lazy val baseProject = project settings(settings:_*)
lazy val finalProject = project settings(settings:_*) dependsOn(leafProject)
lazy val leafProject = project settings(settings:_*) dependsOn(baseProject)


val root = project aggregate(leafProject, baseProject, finalProject)