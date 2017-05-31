/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions
package ci

import java.nio.file.{Files, Path}


trait DownloadableCacheSetup extends CachedCI.Setup {

	/** Loads cache to temporary directory */
	def downloadCache(to: Path): Unit

	/** Stores cache from temporary directory */
	def uploadCache(from: Path): Unit

	// All below big ugly hack, however I needed sth fast that will work!
	private val curretTmpDir: Path = Files.createTempDirectory("downloadableCache")

	/** `doExportCachePart` will export hoarder cache part to given path.
		* Please note that path is global cache so actual part will exported to subdirectory. */
	override def exportCachePart(doExportCachePart: (Path) => Unit): Unit =
		doExportCachePart(curretTmpDir)

	/** `doLoadCachePart` will load hoarder cache part from given path.
		* Please note that path is global cache so actual part will loaded form subdirectory. */
	override def loadCachePart(doLoadCachePart: (Path) => Unit): Unit =
		doLoadCachePart(curretTmpDir)

	/** `doExportCache` will export cache for whole project to provided path */
	override def exportCache(doExportCache: (Path) => Unit): Unit = {
			doExportCache(curretTmpDir)
			uploadCache(curretTmpDir)
	}

	/** `doLoadCache` will load cache for whole project from provided path */
	override def loadCache(doLoadCache: (Path) => Unit): Unit = {
			downloadCache(curretTmpDir)
			doLoadCache(curretTmpDir)
	}
}
