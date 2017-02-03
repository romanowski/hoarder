import org.romanowski.Hoarder._


def settings = org.romanowski.hoarder.tests.PluginTests.testRecompilation ++ useStash

val baseProject = project settings(settings:_*)
val leafProject = project settings(settings:_*) dependsOn(baseProject)

val root = project aggregate(baseProject, leafProject)