import HoarderSettings.autoimport._

import scala.sys.process._
import scala.util.Try
import scala.util.control.NonFatal


val exactVersion = SettingKey[Option[String]]("exactVersion")
exactVersion.in(Global) := Try("git describe --tags --exact-match".!!).toOption

version.in(Global) := {
  try {
    val (version, distance) = exactVersion.value match {
      case Some(version) =>
        version -> 0
      case _ =>
        val output = "git describe --tags".!!.split('-')
        ???
    }
    if (!sys.env.contains("TRAVIS_BRANCH")) {
      val commit = "git rev-parse HEAD".!!
      s"$version-SNAPSHOT-$distance-${commit.take(7)}"
    } else if (distance == 0) version else s"$version-M$distance"
  } catch {
    case NonFatal(e) =>
      print("[ERROR] Unable to compute version. Falling back to 0.1.0-SNAPSHOT")
      e.printStackTrace()
      "0.1.0-SNAPSHOT"
  }
}

crossSbtVersions := Seq("0.13.16", "1.0.2")

def sbtRepo = {
  import sbt.Resolver._
  url("sbt-release-repo", new URL(s"$TypesafeRepositoryRoot/ivy-releases/"))(ivyStylePatterns)
}

def noPublishSettings = Seq(
  publishArtifact := false,
  publishArtifact.in(publishLocal) := true
)

inThisBuild(List(
  licenses := Seq("Apache-style" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://romanowski.github.io/hoarder")),
  developers := List(
    Developer("romanowski", "Krzysztof Romanowski", "romanowski.kr@gmail.com", new URL("http://typosafe.pl"))),
  pomExtra := (
    <scm>
      <url>git@github.com:romanowski/hoarder.git</url>
      <connection>scm:git:git@github.com:romanowski/hoarder.git</connection>
    </scm>
    ),
  organization := "com.github.romanowski",
))

def commonSettings(isSbtPlugin: Boolean = true) = Seq(
  (unmanagedSourceDirectories in Compile) += baseDirectory.value / "src" / "main" / s"sbt_${sbtPrefix.value}",
  (unmanagedSourceDirectories in Test) += baseDirectory.value / "src" / "test" / s"sbt_${sbtPrefix.value}",
  version := version.in(Global).value,
  sbtPlugin := isSbtPlugin,
  publishMavenStyle := true,
  resolvers += sbtRepo,
  scalaVersion := bySbtVersion("2.10.6", "2.12.2").value,
  pomIncludeRepository := { _ => false },

)

val hoarderCore = project.settings(commonSettings(isSbtPlugin = false))

val hoarder = project.settings(commonSettings()).dependsOn(hoarderCore)

val hoarderAmazon = project.in(file("hoarder-amazon")).dependsOn(hoarder).settings(commonSettings())

val hoarderTests = project.dependsOn(hoarderAmazon)
  .settings(commonSettings() ++ noPublishSettings)
  .settings(
    publishLocal := {
      (publishLocal in hoarderCore).value
      (publishLocal in hoarder).value
      (publishLocal in hoarderAmazon).value
      publishLocal.value
    })

val hoarderIntegrationTests = project.dependsOn(hoarder)
  .settings(commonSettings() ++ noPublishSettings)

val root = project.aggregate(hoarderCore, hoarder, hoarderTests, hoarderAmazon, hoarderIntegrationTests).settings(
  taskKey[Unit]("doRelease") := {
    publish.in(hoarderCore).value
    publish.in(hoarder).value
    publish.in(hoarderAmazon).value
  }
).settings(noPublishSettings)

noPublishSettings