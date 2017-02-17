publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

licenses := Seq("Apache-style" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("https://romanowski.github.io/hoarder"))

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