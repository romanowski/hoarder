import org.romanowski.Hoarder._

useStaticCache

exportCacheLocation := target.value.toPath.getParent.resolve("cache")

org.romanowski.hoarder.tests.PluginTests.testRecompilation