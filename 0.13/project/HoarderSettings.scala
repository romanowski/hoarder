import sbt._
import sbt.Keys._


object HoarderSettings extends AutoPlugin {
  override def trigger = allRequirements

  object autoimport {
    val sbtPrefix = SettingKey[String]("hoarder:sbtPrefix")
  }

  import autoimport._

  override val projectSettings = Seq(
    sbtPrefix := sbtVersion.in(pluginCrossBuild).value.split('.').take(2).mkString(".")
  )

}
