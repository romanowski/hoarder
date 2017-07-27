lazy val baseProject = project
lazy val finalProject = project.dependsOn(leafProject)
lazy val leafProject = project.dependsOn(baseProject)

lazy val nested = project.in(file("js") / "baseProject").settings(
	name := "baseProject"
)

val root = project aggregate(leafProject, baseProject, finalProject)