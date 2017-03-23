/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

import java.io.File

object MyCiSetup extends org.romanowski.hoarder.actions.ci.TravisPRValidation{
  override def shouldPublishCaches(): Boolean = new File(".shouldPublishCaches").exists()

  override def shouldUseCache(): Boolean = new File(".shouldUseCache").exists()
}


object MyCiPlugin extends org.romanowski.hoarder.actions.CachedCI.PluginBase(MyCiSetup)