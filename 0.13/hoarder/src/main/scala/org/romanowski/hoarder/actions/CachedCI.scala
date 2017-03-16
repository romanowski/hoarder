package org.romanowski.hoarder.actions

import java.nio.file.{Files, Path, Paths}

import org.romanowski.HoarderSettings._
import org.romanowski.hoarder.core.HoarderEngine
import sbt.Def._
import sbt.Keys._
import sbt._

object CachedCI {

  trait Setup {
    def shouldPublishCaches(): Boolean
    def shouldUseCache(): Boolean

    def loadCache(): Path
    def invalidateCache(): Unit
    def createCachePart(operation: Path => Unit): Unit
  }

  val preBuild = TaskKey[Unit]("preBuild", "TODO")
  val postBuild = TaskKey[Unit]("postBuild", "TODO")

  val currentSetup = SettingKey[Setup]("currentSetup", "TODO")

  private val doLoadCiCaches = TaskKey[Unit]("doLoadCiCaches", "TODO")
  private val doStoreCiCaches = TaskKey[Unit]("doStoreCiCaches", "TODO")


  def projectSettings = Seq()

  def globalSettings = Seq(
    currentSetup := TravisPRValidation(Paths.get(".hoarder-cache")),
    preBuild := {
      val setup = currentSetup.value
      if(setup.shouldUseCache()) doLoadCiCaches.value
    },
    postBuild := {
      val setup = currentSetup.value
      setup.invalidateCache()
      if(setup.shouldPublishCaches()) doStoreCiCaches.value
    }
  )

}

case class TravisPRValidation(cacheDirectory: Path) extends CachedCI.Setup{
  override def shouldPublishCaches(): Boolean = sys.env.get("TRAVIS_EVENT_TYPE") == Some("push")

  override def shouldUseCache(): Boolean = sys.env.get("TRAVIS_EVENT_TYPE") == Some("pull_request")

  override def loadCache(): Path = cacheDirectory

  override def invalidateCache(): Unit = IO.delete(cacheDirectory.toFile)

  override def createCachePart(operation: (Path) => Unit): Unit = {
    Files.createDirectories(cacheDirectory)
    operation(cacheDirectory)
  }
}