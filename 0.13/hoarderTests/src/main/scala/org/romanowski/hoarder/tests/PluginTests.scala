/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski
package hoarder.tests

import java.nio.file.Path

import sbt.Keys._
import sbt._

object PluginTests {

  private val runTestKey = TaskKey[Unit]("")

  def cannotRecompile = file(".cached-compilation").exists()
  def canNoOp = file(".cached-compilation-done").exists()

  private def assertNothingRecompiled(result: Compiler.CompileResult): Compiler.CompileResult = {
    if(cannotRecompile) {
      if (canNoOp) {
        if (result.hasModified) throw new RuntimeException(s"Compilation wasn't no-op in ${result.setup.output}")
      } else throw new RuntimeException(s"Compilation was triggered in ${result.setup.output}")
    }
    result
  }

  private def runTest = Def.task {
    val classesDir = classDirectory.value

    assertNothingRecompiled(manipulateBytecode.value)

    val allClasses = (classesDir ** "*.class").get
    assert(allClasses.nonEmpty, s"No classes present in $classesDir!")

    streams.value.log.success(s"Nothing modified in $classesDir")
  }

  def perConfigSettings = Seq(
    runTestKey <<= runTest,
    manipulateBytecode ~= assertNothingRecompiled
  )

  def testRecompilation =
    inConfig(Compile)(perConfigSettings) ++ inConfig(Test)(perConfigSettings) ++ Seq(
      TaskKey[Unit]("testCacheImport") := {
        streams.value.log.success(s"Testing for ${name.value}")
        runTestKey.in(Compile).value
        runTestKey.in(Test).value
      },
      scalaVersion := "2.11.8"
    )


}
