package org.romanowski.hoarder.actions

import sbt._

trait StashKeys {
  val stashKey = InputKey[Unit]("stash",
    "Export results of current compilation to global location.")
  val stashApplyKey = InputKey[Unit]("stashApply",
    "Load exported compilation results from global location.")
  val stashCleanKey = InputKey[Unit]("stashClean",
    "Clean exported compilation results from global location.")

  val stashPrefixKey = TaskKey[String]("stashPrefix",
    "Prefix used to stash code. By default point to scala binary version")

  val defaultVersionLabel = SettingKey[String]("hoarder:defaultVersionLabel",
    "Default version label for stash/stashApply")
  val defaultProjectLabel = SettingKey[String]("hoarder:defaultProjectLabel",
    "Default root project label for stash/stashApply")
  val globalStashLocation = TaskKey[File]("hoarder:globalStashLocation",
    "Root directory where exported artifacts are kept.")
}