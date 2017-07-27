import sbt.dsl._

lazy val baseProject = project
lazy val finalProject = project dependsOn(leafProject)
lazy val leafProject = project dependsOn(baseProject)


val root = project aggregate(leafProject, baseProject, finalProject)