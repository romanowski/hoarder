/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.actions

import java.nio.file.Path

import org.romanowski.HoarderKeys._
import org.romanowski.HoarderKeys.failOnMissing
import org.romanowski.coursier.CoursierResolver
import org.romanowski.hoarder.core.ExportedCache
import org.romanowski.hoarder.core.HoarderCompilationCache
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Def._
import sbt.Keys._
import sbt._
import org.romanowski.hoarder.core.SbtTypes._
import org.romanowski.hoarder.core.ZincSpecific.AnalysisContents

object CachedRelease extends HoarderEngine {

  def settings = Seq(
    setupHoarderArtifacts,
    setupPackagedArtifacts,
    artifacts ++= hoarderArtifacts.value.values.toSeq,
    loadRelease := doLoadCache.evaluated,
    failOnMissing.in(ThisBuild) := true
  )


  override protected def compilationCache(cacheSetup: CacheSetup, globalCacheLocation: Path) =
    new HoarderCompilationCache(cacheSetup, globalCacheLocation){
      override protected def exportBinaries(from: AnalysisContents): Unit = {}
    }

  private val hoarderArtifacts = SettingKey[Map[Configuration, Artifact]]("hoarder:private:artifacts", "Internal")

  private def hoarderType(configuration: Configuration) = s"${configuration.name}-hoarder-cache"

  private def setupHoarderArtifacts = hoarderArtifacts := enabledConfigurations.value.map {
    configuration =>
      configuration -> createArtifact(
        name = name.value,
        `type` = hoarderType(configuration),
        extension = "zip",
        classifier = Option(hoarderType(configuration)),
        configurations = Seq(configuration)
      )
  }(collection.breakOut)

  private def setupPackagedArtifacts = packagedArtifacts ++= {
    assert(zipAnalysisCache.value, "Cannot publish non-zipped artifacts!")

    val globalCache = crossTarget.value.toPath.resolve("inc-artifact")
    val artifacts = hoarderArtifacts.value

    val packagedArtifacts = for {
      ExportCacheSetup(cacheSetup, result) <- exportCacheSetups.value
      artifact <- artifacts.get(cacheSetup.configuration) //TODO do not publish tests artifacts if tests are not published?
      ExportedCache(analysis, _) = exportCacheTaskImpl(cacheSetup, result, globalCache)
    } yield artifact -> analysis.toFile

    packagedArtifacts.toMap
  }

  private val versionParser = {
    import sbt.complete.Parser._
    import sbt.complete.Parsers._

    Space ~> token(StringBasic, "<version>")
  }

  private class Loader(baseModule: ModuleID, coursierResolver: CoursierResolver, streams: TaskStreams, failOnMissing: Boolean)
    extends (CacheSetup => Unit) {

    override def apply(cacheSetup: CacheSetup): Unit = {
      val configurationName = cacheSetup.configuration.name
      val configurationDesc = baseModule.withConfigurations(Option(s"$configurationName->$configurationName"))
      val resolvedArtifacts = coursierResolver.resolve(configurationDesc)

      def artifactFor(artifactType: String): Option[URL] = {
        def failureMessage =
          s"[${cacheSetup.name}] Unable to find artifact (configuration: $configurationName type: $artifactType)."

        resolvedArtifacts.get(artifactType) match {
          case None if failOnMissing =>
            throw new RuntimeException(failureMessage)
          case None =>
            streams.log.error(failureMessage)
            None
          case resolved =>
            resolved
        }
      }

      for {
        binaries <- artifactFor("jar")
        analysis <- artifactFor(hoarderType(cacheSetup.configuration))
      } {
        val globalCacheLocation = streams.cacheDirectory.toPath
        val cacheLocation = globalCacheLocation.resolve(cacheSetup.relativeCacheLocation)
        IO.download(binaries, cacheLocation.resolve(classesZipFileName).toFile)
        IO.download(analysis, cacheLocation.resolve(analysisCacheZipFileName).toFile)
        importCacheTaskImpl(cacheSetup, globalCacheLocation)
      }
    }
  }

  private def doLoadCache = Def.inputTask[Unit] {
    val version = versionParser.parsed
    val sbtResolvers = publishTo.value.toSeq ++ externalResolvers.value

    val loadCache = new Loader(
      baseModule = projectID.value.intransitive().withRevision(version).withExplicitArtifacts(Vector.empty),
      coursierResolver = CoursierResolver(streams.value.log, scalaVersion.value, scalaBinaryVersion.value, sbtResolvers),
      streams = streams.value,
      failOnMissing = failOnMissing.value
    )

    importCacheSetups.value.foreach(loadCache)
  }
}
