version.in(Global) := "1.0.1-SNAPSHOT"

def commonSettings =  Seq(
  version := version.in(Global).value,
  scalaVersion := "2.10.6",
  sbtPlugin := true,
  organization := "com.github.romanowski",
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
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

val hoarder = project.settings(commonSettings: _*)

val hoarderAmazon = project.in(file("hoarder-amazon")).dependsOn(hoarder).settings(commonSettings: _*)

val hoarderTests = project.dependsOn(hoarderAmazon)
  .settings(commonSettings: _*)
  .settings(
    publishLocal := {
      (publishLocal in hoarder).value
      (publishLocal in hoarderAmazon).value
      publishLocal.value
    })

val root = project aggregate(hoarder, hoarderTests, hoarderAmazon)
