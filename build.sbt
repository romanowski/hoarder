import HoarderSettings.autoimport._

version.in(Global) := "1.0.1-SNAPSHOT"
crossSbtVersions := Seq("0.13.16", "1.0.0-RC3")

def commonSettings(isSbtPlugin: Boolean = true) =  Seq(
  (unmanagedSourceDirectories in Compile) += baseDirectory.value / "src" / "main" / s"sbt_${sbtPrefix.value}",
  (unmanagedSourceDirectories in Test) += baseDirectory.value / "src" / "test" / s"sbt_${sbtPrefix.value}",
  version := version.in(Global).value,
  sbtPlugin := isSbtPlugin,
  organization := "com.github.romanowski",
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scalaVersion := cross("2.10.6", "2.12.2").value,
  pomIncludeRepository := { _ => false },
  licenses := Seq("Apache-style" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://romanowski.github.io/hoarder")),
  pomExtra := (
    <scm>
      <url>git@github.com:romanowski/hoarder.git</url>
      <connection>scm:git:git@github.com:romanowski/hoarder.git</connection>
    </scm>
      <developers>
        <developer>
          <id>romanowski</id>
          <name>Krzysztof Romanowski</name>
          <url>http://typosafe.pl</url>
        </developer>
      </developers>)
)

val hoarderCore = project.settings(commonSettings(isSbtPlugin = false))

val hoarder = project.settings(commonSettings()).dependsOn(hoarderCore)

val hoarderAmazon = project.in(file("hoarder-amazon")).dependsOn(hoarder).settings(commonSettings())

val hoarderTests = project.dependsOn(hoarderAmazon)
  .settings(commonSettings())
  .settings(
    publishLocal := {
      (publishLocal in hoarderCore).value
      (publishLocal in hoarder).value
      (publishLocal in hoarderAmazon).value
      publishLocal.value
    })

val root = project aggregate(hoarderCore, hoarder, hoarderTests, hoarderAmazon)

