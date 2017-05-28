org.romanowski.hoarder.actions.CachedRelease.settings

publishTo := Some(Resolver.file("my-local", file(".") / "repo")(Resolver.defaultIvyPatterns))

publishMavenStyle := false
publishArtifact in Test := true
