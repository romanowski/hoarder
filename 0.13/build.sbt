val hoarder = project

val hoarderTests = project dependsOn hoarder settings(
  publishLocal := {
    (publishLocal in hoarder).value
   publishLocal.value
  })

val root = project aggregate(hoarder, hoarderTests)
