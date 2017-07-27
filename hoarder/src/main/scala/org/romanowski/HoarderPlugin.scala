/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski

import java.nio.file.Path
import java.nio.file.Paths

import org.romanowski.hoarder.actions.CachedCiKeys
import org.romanowski.hoarder.actions.CachedReleaseKeys
import org.romanowski.hoarder.actions.Stash
import org.romanowski.hoarder.actions.StashKeys
import org.romanowski.hoarder.core.CleanClasses
import org.romanowski.hoarder.core.CleanOutputMode
import org.romanowski.hoarder.core.SbtTypes.CompilationResult
import sbt.Keys._
import sbt._


object HoarderKeys extends StashKeys with CachedCiKeys with CachedReleaseKeys {
	private[romanowski] def internalTask[T: Manifest](name: String) =
		TaskKey[T](s"hoarder:internal:$name", s"Internal hoarder task: $name. Please do not use.")


	// TODO we may still misss some sources directories
	// Find better way for finding source dir roots
	case class CacheSetup(allSources: Seq[File],
		                    sourceRoots: Seq[File],
	                      classpath: Classpath,
	                      classesRoot: Path,
	                      projectRoot: Path,
	                      analysisFile: File,
	                      relativeCacheLocation: Path,
	                      overrideExistingCache: Boolean,
	                      cleanOutputMode: CleanOutputMode,
	                      zipAnalysisFile: Boolean,
	                      configuration: Configuration,
	                      name: String
	                     ) {
		def cacheLocation(root: Path) = root.resolve(relativeCacheLocation)
	}

	case class ExportCacheSetup(cacheSetup: CacheSetup, compilationResult: CompilationResult)

	case class ExportedCache(analysis: Path, binaries: Option[Path])

	val cleanOutputMode = SettingKey[CleanOutputMode]("hoarder:cleanOutputMode", "What should be cleaned prior to cache extraction")
	val zipAnalysisCache = SettingKey[Boolean]("hoarder:zipAnalysisFile", "Determines if analysis file will be zipped or not")
	val overrideExistingCache = SettingKey[Boolean]("hoarder:overrideExistingCache", "Override existing stash")
	val enabledConfigurations = SettingKey[Seq[Configuration]]("hoarder:enabledConfigurations",
		"Configuration that hoarder will use")


	def withConfiguration(config: Configuration): Seq[Setting[_]] = HoarderPlugin.includeConfiguration(config)

	private[romanowski] val importCacheSetups = internalTask[Seq[CacheSetup]]("importCacheSetups")
	private[romanowski] val exportCacheSetups = internalTask[Seq[ExportCacheSetup]]("exportCacheSetups")
	private[romanowski] val perConfigurationSetup = internalTask[CacheSetup]("perConfigurationSetup")
	private[romanowski] val perConfigurationExportSetup = internalTask[ExportCacheSetup]("perConfigurationExportSetup")
}

object HoarderPlugin extends AutoPlugin {

	override def projectSettings = defaultPerProject ++ Stash.settings

	override def globalSettings: Seq[_root_.sbt.Def.Setting[_]] = Stash.globalSettings ++ defaultsGlobal

	override def trigger: PluginTrigger = AllRequirements

	object autoImport {
		val hoarder = HoarderKeys
	}

	import HoarderKeys._


	private[romanowski] def includeConfiguration(config: Configuration): Seq[Setting[_]] = {
		inConfig(config)(Seq(
			perConfigurationSetup := projectSetupFor.value,
			perConfigurationExportSetup := ExportCacheSetup(perConfigurationSetup.value, compileIncremental.value)
		)) ++ Seq(
			importCacheSetups += perConfigurationSetup.in(config).value,
			exportCacheSetups += perConfigurationExportSetup.in(config).value,
			enabledConfigurations += config
		)
	}

	private def projectSetupFor = Def.task[CacheSetup] {
		val projectName = name.value
		val relativeCacheLocation = {
			val rootDir = Paths.get(".").toAbsolutePath.getParent
			val baseDir = baseDirectory.value.toPath.toAbsolutePath
			val relativePath =
				if (rootDir == baseDir) Paths.get(projectName)
				else {
					assert(baseDir.startsWith(rootDir))
					rootDir.relativize(baseDir)
				}
			relativePath.resolve(configuration.value.name)
		}

		CacheSetup(
			allSources = sources.value,
			sourceRoots = sourceDirectories.value,
			classpath = externalDependencyClasspath.value,
			classesRoot = classDirectory.value.toPath,
			projectRoot = baseDirectory.value.toPath,
			analysisFile = (streams in compileIncSetup).value.cacheDirectory / compileAnalysisFilename.value,
			relativeCacheLocation = relativeCacheLocation,
			overrideExistingCache = overrideExistingCache.value,
			cleanOutputMode = cleanOutputMode.value,
			zipAnalysisFile = zipAnalysisCache.value,
			configuration = configuration.value,
			name = name.value
		)
	}


	def defaultsGlobal = Seq(
		cleanOutputMode := CleanClasses,
		zipAnalysisCache := true,
		overrideExistingCache := false,
		importCacheSetups := Nil,
		exportCacheSetups := Nil
	)

	def defaultPerProject =
		Seq(importCacheSetups := Nil, exportCacheSetups := Nil, enabledConfigurations := Nil) ++
			includeConfiguration(Compile) ++
			includeConfiguration(Test)
}
