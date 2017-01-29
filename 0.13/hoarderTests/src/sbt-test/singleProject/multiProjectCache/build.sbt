import org.romanowski.Hoarder._


def settings = Seq(
  exportCacheLocation := target.value.toPath.getParent.resolve("cache")
) ++ org.romanowski.hoarder.tests.PluginTests.testRecompilation ++ useStaticCache

val baseProject = project settings(settings:_*)
val leafProject = project settings(settings:_*) dependsOn(baseProject)

val root = project aggregate(baseProject, leafProject)