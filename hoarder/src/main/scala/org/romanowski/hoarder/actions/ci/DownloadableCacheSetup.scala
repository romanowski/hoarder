/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions
package ci

import java.nio.file.{Files, Path}

import org.romanowski.hoarder.actions.CachedCI.CacheProgress
import sbt.IO

trait DownloadableCacheSetup extends CachedCI.Setup {

	/** Loads cache to temporary directory and returns path where actual cache is */
	def downloadCache(to: Path): Path

	/** Stores cache from temporary directory */
	def uploadCache(from: Path): Unit

	/** `doExportCache` will export cache for whole project to provided path */
	override def exportCache(): CacheProgress = new CacheProgress {
		lazy val currentTmpDir: Path = Files.createTempDirectory("downloadableCache")

		override def done(): Unit = {
			uploadCache(currentTmpDir)
			IO.delete(currentTmpDir.toFile)
		}

		override def nextPart[T](op: (Path) => T): T = op(currentTmpDir)
	}

	/** `doLoadCache` will load cache for whole project from provided path */
	override def loadCache(): CacheProgress = {
		val currentTmpDir = Files.createTempDirectory("downloadableCache")
		val downloadedCache = downloadCache(currentTmpDir)
		new CacheProgress {
			override def done(): Unit = IO.delete(downloadedCache.toFile)

			override def nextPart[T](op: (Path) => T): T = op(downloadedCache)
		}

	}
}
