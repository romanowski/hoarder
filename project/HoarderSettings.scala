import sbt._
import sbt.Keys._


object HoarderSettings extends AutoPlugin {
  override def trigger = allRequirements

  object autoimport {
    val sbtPrefix = SettingKey[String]("hoarder:sbtPrefix")

    def bySbtVersion[T](`0.13`: T, `1.0`: T) = Def.setting {
      sbtPrefix.value  match {
        case "0.13" =>
          `0.13`
        case "1.0" =>
          `1.0`
      }
    }
  }

  import autoimport._

  override val projectSettings = Seq(
    sbtPrefix := sbtVersion.in(pluginCrossBuild).value.split('.').take(2).mkString(".")
  )

}
