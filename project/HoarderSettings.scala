import sbt.Keys._
import sbt._


object HoarderSettings extends AutoPlugin {
  override def trigger = allRequirements

  object autoimport {
    val sbtPrefix = SettingKey[String]("hoarder:sbtPrefix")
    val isLegacySbt = SettingKey[Boolean]("hoarder:isLegacySbt")
    val zincVersion = "1.2.5"
  }

  import autoimport._

  override val projectSettings = Seq(
    sbtPrefix :=  (sbtVersion.in(pluginCrossBuild).value.split('.').take(2).mkString(".") match {
      case "0.13" => "0.13"
      case recent if recent.startsWith("1.") =>
        "1.0"
    }),
    isLegacySbt := sbtPrefix.value == "0.13"
  )

}
