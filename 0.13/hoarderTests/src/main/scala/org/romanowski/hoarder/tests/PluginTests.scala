package org.romanowski
package hoarder.tests

import java.nio.file.Path

import sbt.Keys._
import sbt._

object PluginTests {

  def testRecompilation = Seq(TaskKey[Unit]("testCacheImport") := {
    val result = (manipulateBytecode in Compile).value
    assert(!result.hasModified)

    val classesDir = (classDirectory in Compile).value
    val allClasses = (classesDir ** "*.class").get
    assert(allClasses.nonEmpty)
  },
    scalaVersion := "2.11.8"
  )


}
