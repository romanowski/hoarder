addSbtPlugin("com.jsuereth" %% "sbt-pgp" % "1.1.0-M1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")

libraryDependencies += {
  if (sbtVersion.value.startsWith("1.0")) "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
  else "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
}

resolvers += {
  import sbt.Resolver._
  url("sbt-release-repo", new URL(s"$TypesafeRepositoryRoot/ivy-releases/"))(ivyStylePatterns)
}


