package org.romanowski
package hoarder.tests

import java.nio.file.Path

import sbt.Keys._
import sbt._

object PluginTests {

  private val runTestKey = TaskKey[Unit]("")

  private def runTest = Def.task {
    val result = manipulateBytecode.value
    assert(!result.hasModified, "Compilation after import was no no-op!")

    val classesDir = classDirectory.value
    val allClasses = (classesDir ** "*.class").get
    assert(allClasses.nonEmpty, s"No classes present in $classesDir!")

    streams.value.log.success(s"Nothing modified in $classesDir")
  }

  def testRecompilation =
    inConfig(Compile)(runTestKey <<= runTest) ++
      inConfig(Test)(runTestKey <<= runTest) ++ Seq(
      TaskKey[Unit]("testCacheImport") := {
        streams.value.log.success(s"Testing for ${name.value}")
        runTestKey.in(Compile).value
        runTestKey.in(Test).value
      },
      scalaVersion := "2.11.8"
    )


}
