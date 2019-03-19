name := "hoarder-tests"

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq(
    "-Xmx2024M",
    "-XX:MetaspaceSize=256M",
    "-XX:MaxMetaspaceSize=512M",
    "-Dplugin.version=" + version.value
  )
}
scriptedBufferLog := false
