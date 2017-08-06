/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.coursier

import java.io.File

import sbt.Logger
import sbt.ModuleID
import sbt.Resolver
import sbt.URL

case class CoursierResolver(log: Logger, scalaVersion: String, scalaBinaryVersion: String, resolvers: Seq[Resolver]) {
  private lazy val ivyHome = sys.props.getOrElse(
    "ivy.home",
    new File(sys.props("user.home")).toURI.getPath + ".ivy2"
  )

  private lazy val sbtIvyHome = sys.props.getOrElse(
    "sbt.ivy.home",
    ivyHome
  )

  private lazy val ivyProperties = Map(
    "ivy.home" -> ivyHome,
    "sbt.ivy.home" -> sbtIvyHome
  ) ++ sys.props

  private lazy val repositories = resolvers.flatMap(r => FromSbt.repository(r, ivyProperties, log, None))

  def resolve(sbtModule: ModuleID): Map[String, URL] = {
    import coursier._

    val dependencies = FromSbt.dependencies(sbtModule, scalaVersion, scalaBinaryVersion)
      .map(_._2.copy(transitive = false))

    val start = Resolution(dependencies.toSet)

    val fetch = Fetch.from(repositories, Cache.fetch())

    val resolution = start.process.run(fetch).unsafePerformSync

    val resolved = resolution.dependencyArtifacts.map {
      case (artifactDependency, artifact) =>
      artifact.`type` -> new URL(artifact.url)
    }

    resolved.toMap
  }
}