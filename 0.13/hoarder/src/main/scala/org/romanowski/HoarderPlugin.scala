package org.romanowski

import org.romanowski.hoarder.actions.Stash
import org.romanowski.hoarder.location.StaticInteractiveLocation
import sbt.{AllRequirements, AutoPlugin, PluginTrigger}


object HoarderPlugin extends AutoPlugin {
  override def projectSettings = HoarderCommonSettings.defaults ++ Stash.settings ++ StaticInteractiveLocation.settings

  override def trigger: PluginTrigger = AllRequirements
}
