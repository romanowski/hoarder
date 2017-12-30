import HoarderSettings.autoimport._

/*version.in(Global) := Option(System.getenv("HOARDER_CI_VERSION"))
  .getOrElse("1.0.3-SNAPSHOT")*/

crossSbtVersions := Seq("0.13.16", "1.0.2")

def sbtRepo = {
  import sbt.Resolver._
  url("sbt-release-repo", new URL(s"$TypesafeRepositoryRoot/ivy-releases/"))(ivyStylePatterns)
}

def noPublishSettings = Seq(
  releaseEarly := {},
  //publishArtifact := false
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
  pgpPublicRing := file("ci-scripts/pubring.asc"),
  pgpSecretRing := file("ci-scripts/secring.asc"),
  releaseEarlyEnableLocalReleases := true,
  releaseEarlyWith := SonatypePublisher
))

def commonSettings(isSbtPlugin: Boolean = true) =  Seq(
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