libraryDependencies += {
  if (sbtVersion.value.startsWith("1.0")) "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
  else "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
}


