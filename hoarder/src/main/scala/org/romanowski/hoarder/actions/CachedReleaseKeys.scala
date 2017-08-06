/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import sbt.InputKey
import sbt.SettingKey

trait CachedReleaseKeys {
  val loadRelease = InputKey[Unit]("loadRelease", "Load cached release from specified version.")
  val failOnMissing = SettingKey[Boolean]("hoarder:failOnMissing", "Fail task if there are missing artifacts")
}
