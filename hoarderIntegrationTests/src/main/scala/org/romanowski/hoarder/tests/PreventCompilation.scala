import sbt._
import Keys._
import sbt.plugins.CorePlugin
import sbt.plugins.JvmPlugin
import org.romanowski.HoarderKeys.cachedCiSetup


object PreventCompilation extends AutoPlugin {

  // so we can rewrite default tasks, e.g. compile
  override def requires: Plugins = CorePlugin && JvmPlugin

  def perConfig = Seq(
    manipulateBytecode := {
      val res = manipulateBytecode.value

      def isIgnored = { // TODO Hoarder#30
        name.value.startsWith("testing_") || // TODO Hoarder#30
          name.value == "testutil" || // TODO Hoarder#30
          name.value == "core" // BuildInfo gets generated...
      }

      res.hasModified match {
        case true if cachedCiSetup.value.shouldUseCache() && !isIgnored =>
          throw new RuntimeException(s"Compilation wasn't no-op after import for ${name.value}/${configuration.value}")
        case true if !cachedCiSetup.value.shouldUseCache() || isIgnored =>
          streams.value.log.success(s"Compilation result is ignored.")
        case _ =>
          streams.value.log.success(s"Compilation was not triggered for ${name.value}.")
      }
      res
    }
  )

  override def trigger: PluginTrigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] = inConfig(Compile)(perConfig) ++ inConfig(Test)(perConfig)

  override def globalSettings: Seq[Setting[_]] =
    Seq(TaskKey[Unit]("preventCompilationStatus") := streams.value.log.success("Prevent Compilation present!"))
}