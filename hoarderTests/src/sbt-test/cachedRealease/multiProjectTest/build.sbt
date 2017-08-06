def baseSettings =
  org.romanowski.hoarder.actions.CachedRelease.settings ++ Seq(
    publishTo := Some(Resolver.file("my-local", file(".")  / "repo")(Resolver.defaultIvyPatterns)),
    publishMavenStyle := false,
    publishArtifact in Test := true
  )

lazy val baseProject = project.settings(baseSettings: _*)
lazy val finalProject = project.settings(baseSettings: _*).dependsOn(leafProject)
lazy val leafProject = project.settings(baseSettings: _*).dependsOn(baseProject)

val root = project.settings(baseSettings: _*).aggregate(leafProject, baseProject, finalProject)

baseSettings