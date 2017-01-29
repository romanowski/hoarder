name := "hoarder"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.6"

sbtPlugin := true

organization := "org.romanowski"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.11.4" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6"

libraryDependencies +="junit" % "junit" % "4.11"