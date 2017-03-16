/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski

import java.nio.file.{Files, Path, Paths}

import org.romanowski.hoarder.core._
import sbt.Keys._
import sbt._
import SbtTypes.CompilationResult

object HoarderSettings {

  case class CacheSetup(sourceRoots: Seq[File],
                        classpath: Classpath,
                        classesRoot: Path,
                        projectRoot: Path,
                        analysisFile: File,
                        relativeCacheLocation: Path,
                        overrideExistingCache: Boolean,
                        cleanOutputMode: CleanOutputMode
                       ){
    def cacheLocation(root: Path) = root.resolve(relativeCacheLocation)
  }

  case class ExportCacheSetup(cacheSetup: CacheSetup, compilationResult: CompilationResult)


  val cleanOutputMode = SettingKey[CleanOutputMode]("cleanOutputMode", "What should be cleaned prior to cache extraction")
  val overrideExistingCache = SettingKey[Boolean]("overrideExistingCache", "Override existing stash")

  private[romanowski] val importCacheSetups = TaskKey[Seq[CacheSetup]]("hoarder:internal:importCacheSetups", "Internal")
  private[romanowski] val exportCacheSetups = TaskKey[Seq[ExportCacheSetup]]("hoarder:internal:exportCacheSetups", "Internal")
  private[romanowski] val perConfigurationSetup =
    TaskKey[CacheSetup]("hoarder:internal:perConfigurationSetup", "Internal")
  private[romanowski] val perConfigurationExportSetup =
    TaskKey[ExportCacheSetup]("hoarder:internal:perConfigurationExportSetup", "Internal")

  private def projectSetupFor = Def.task[CacheSetup] {
    CacheSetup(
      sourceRoots = managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
      classpath = externalDependencyClasspath.value,
      classesRoot = classDirectory.value.toPath,
      projectRoot = baseDirectory.value.toPath,
      analysisFile = (streams in compileIncSetup).value.cacheDirectory / compileAnalysisFilename.value,
      relativeCacheLocation = Paths.get(name.value).resolve(configuration.value.name),
      overrideExistingCache = overrideExistingCache.value,
      cleanOutputMode = cleanOutputMode.value
    )
  }


  def defaultsGlobal = Seq(
      cleanOutputMode := CleanClasses,
      overrideExistingCache := false,
      importCacheSetups := Nil,
      exportCacheSetups := Nil
    )

  def defaultPerProject =
    Seq(importCacheSetups := Nil, exportCacheSetups := Nil) ++
    includeConfiguration(Compile) ++
    includeConfiguration(Test)

  def includeConfiguration(config: Configuration) = {
    inConfig(config)(Seq(
      perConfigurationSetup := projectSetupFor.value,
      perConfigurationExportSetup := ExportCacheSetup(perConfigurationSetup.value, compileIncremental.value)
    )) ++ Seq(
      importCacheSetups += perConfigurationSetup.in(config).value,
      exportCacheSetups += perConfigurationExportSetup.in(config).value
    )
  }
}

