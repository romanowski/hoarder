name := "hoarder-tests"

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq(
    "-Xmx1024M",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5015",
    "-Dplugin.version=" + version.value
  )
}

scriptedBufferLog := false
