/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski
package hoarder.tests

import org.romanowski.hoarder.core.SbtTypes._
import sbt.Keys._
import sbt._
import sbt.complete.Parser

object PluginTests extends AutoPlugin {

  object autoImport {
    def testRecompilationIn(configurations: Configuration*) = Seq(
      testCacheImportKey := streams.value.log.success(s"Testing for ${name.value}"),
      scalaVersion := "2.11.8"
    ) ++ configurations.flatMap(testConfiguration)
  }

  import autoImport._

  override def trigger: PluginTrigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] =
    Seq(changeLineEndings, aggregate.in(runTestKey) := true,
      testIncCompilation, allCompilationKey := Nil) ++
      testRecompilationIn(Compile, Test)

  private val runTestKey = TaskKey[Unit]("hoarder:test:runTest")
  private val testCacheImportKey = TaskKey[Unit]("testCacheImport")
  private val allCompilationKey = TaskKey[Seq[CompilationResult]]("test:allIncCompileResult")

  private def cannotRecompileFile = file(".cached-compilation")
  def cannotRecompile = cannotRecompileFile.exists()
  private val shouldRestoreCannotRecompileFile = Def.task[Boolean]{
    if(canNoOp) cannotRecompileFile.delete() else false
  }

  private def noOpFile = file(".cached-compilation-done")
  private def canNoOp = noOpFile.exists()

  private def assertNothingRecompiled(result: CompilationResult): CompilationResult = {
    if (cannotRecompile) {
      if (canNoOp) {
        if (result.hasModified) throw new RuntimeException(s"Compilation wasn't no-op in ${result.setup.output}")
      } else throw new RuntimeException(s"Compilation was triggered in ${result.setup.output}")
    }
    result
  }

  private def runTest = Def.task {
    val classesDir = classDirectory.value
    val log = streams.value.log

    assertNothingRecompiled(manipulateBytecode.value)

    val allClasses = (classesDir ** "*.class").get
    val allSources = sources.value
    assert(allClasses.nonEmpty || allSources.isEmpty, s"No classes present in $classesDir!")

    log.success(s"Nothing modified in $classesDir")

  }

  def perConfigSettings = Seq(
    runTestKey := runTest.value,
    manipulateBytecode ~= assertNothingRecompiled
  )

  def testConfiguration(configuration: Configuration) =
    inConfig(configuration)(perConfigSettings) ++
      Seq(testCacheImportKey := {
        testCacheImportKey.value
        runTestKey.in(configuration).value
      }) ++ Seq(allCompilationKey += compileIncremental.in(configuration).value)

  private def changeLineEndingsForFile(content: String): String = {
    val fileLines = content.linesIterator.toSeq

    val Linux = fileLines.mkString("\n")
    val Windows = fileLines.mkString("\r\n")
    val Macos = fileLines.mkString("\r")

    content match {
      case Linux => Windows
      case Windows => Macos
      case Macos => Linux
    }
  }

  def changeLineEndings = TaskKey[Unit]("changeLineEndings") := {
    (file(".") ** "*.scala").get.foreach {
      file =>
        val content = IO.read(file)
        val changed = changeLineEndingsForFile(content)
        IO.write(file, changed)
    }
  }

  private val fileNameParser: Parser[Set[String]] = {
    import sbt.complete.Parser._
    import sbt.complete.Parsers._

    (Space.+ ~> StringBasic).map(v => s"$v.scala").*.map(_.toSet)
  }

  private val testIncCompilationFiles = Def.inputTask {
    fileNameParser.parsed
  }



  def testIncCompilation = InputKey[Unit]("testIncCompilation") := {
     (for {
       shouldRestoreNoOp <- shouldRestoreCannotRecompileFile.taskValue
       filesNames <- testIncCompilationFiles.parsed
       streams <- streams.taskValue
       date <- task(System.currentTimeMillis())
       results <- allCompilationKey.taskValue
    } yield {
       val recompiled = results.flatMap(_.compiledAfter(date)).toSet
      assert(recompiled == filesNames, s"Expected $filesNames to be recompiled but got: $recompiled instead.")

       streams.log.success(s"Only $recompiled was recompiled")
       if(shouldRestoreNoOp) IO.touch(cannotRecompileFile)

      recompiled
    }).value
  }
}
