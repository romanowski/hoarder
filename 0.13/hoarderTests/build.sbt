name := "hoarder-tests"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.6"

sbtPlugin := true

organization := "org.romanowski"

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5015")
}

scriptedBufferLog := false
