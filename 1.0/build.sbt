name := "hoarder"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.1"

sbtPlugin := true

organization := "com.github.romanowski"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "src" / "main" / "sbt_1.0"


// Reuse code from 0.13 branch
scalaSource in Compile := baseDirectory.value / ".." / "0.13" / "hoarder" / "src" / "main" / "scala"
scalaSource in Test := baseDirectory.value / ".." / "0.13" / "hoarder" / "src" / "test" / "scala"