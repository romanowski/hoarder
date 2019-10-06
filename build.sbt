import HoarderSettings.autoimport._
import sbt.file

import scala.sys.process._
import scala.util.Try
import scala.util.control.NonFatal


lazy val exactVersion = Try("git describe --tags --exact-match".!!.trim).toOption
val shouldReleaseAsSnapshot = exactVersion.isEmpty || !sys.env.contains("TRAVIS_BRANCH")

version.in(Global) := sys.env.get("HOARDER_CI_VERSION").getOrElse {
  try exactVersion match {
    case Some(version) =>
      if (shouldReleaseAsSnapshot) s"$version-SNAPSHOT" else version
    case _ =>
      val output = "git describe --tags".!!.trim.split('-')
      val version = output.dropRight(2).mkString("-")
      val distance = output.takeRight(2).head
      s"$version-M$distance-SNAPSHOT"
  } catch {
    case NonFatal(e) =>
      println("[ERROR] Unable to compute version. Falling back to 0.1.0-SNAPSHOT")
      e.printStackTrace()
      "0.1.0-SNAPSHOT"
  }
}

crossSbtVersions := Seq("0.13.18", "1.0.2")

inThisBuild {
  resolvers += {
    import sbt.Resolver._
    url("sbt-release-repo", new URL(s"$TypesafeRepositoryRoot/ivy-releases/"))(ivyStylePatterns)
  }
}

def noPublishSettings = Seq(
  publishTo := Some(Resolver.file("my-local", file(".") / "repo")(Resolver.defaultIvyPatterns)),
  publishLocalConfiguration ~= (_.withOverwrite(true))
)

def publishSettings = Seq(
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
  publishTo := {
    import Opts.resolver._
    Some(if (shouldReleaseAsSnapshot) sonatypeSnapshots else sonatypeStaging)
  },
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  PgpKeys.publishSignedConfiguration := PgpKeys.publishSignedConfiguration.value.withOverwrite(isSnapshot.value),
  pgpSecretRing := file("private-secring.asc"),
  pgpPublicRing := file("private-pubring.asc"),
  updateOptions := updateOptions.value.withGigahorse(false)
)

def commonSettings(isSbtPlugin: Boolean = true, shouldPublish: Boolean = true) = Seq(
  (unmanagedSourceDirectories in Compile) += baseDirectory.value / "src" / "main" / s"sbt_${sbtPrefix.value}",
  (unmanagedSourceDirectories in Test) += baseDirectory.value / "src" / "test" / s"sbt_${sbtPrefix.value}",
  version := version.in(Global).value,
  sbtPlugin := isSbtPlugin,
  scalaVersion := (if(isLegacySbt.value) "2.10.6" else "2.12.2"),
) ++ publishSettings

val hoarderCore = project.settings(commonSettings(isSbtPlugin = false))

val hoarder = project.settings(commonSettings()).dependsOn(hoarderCore)

val hoarderAmazon = project.in(file("hoarder-amazon")).dependsOn(hoarder).settings(commonSettings())

val hoarderTests = project.enablePlugins(SbtPlugin).dependsOn(hoarderAmazon)
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

publishSettings

noPublishSettings

addCommandAlias("finalizeRelease", if (shouldReleaseAsSnapshot) "state" else "sonatypeRelease")