/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski

import org.romanowski.hoarder.actions.Stash
import org.romanowski.hoarder.location.StaticInteractiveLocation
import sbt.{AllRequirements, AutoPlugin, PluginTrigger}


object HoarderPlugin extends AutoPlugin {
  import HoarderCommonSettings._

  override def projectSettings = Stash.settings

  override def globalSettings: Seq[_root_.sbt.Def.Setting[_]] = StaticInteractiveLocation.settings ++ defaultsGlobal

  override def trigger: PluginTrigger = AllRequirements
}
