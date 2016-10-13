package pl.typosafe.demeter

import java.io.File

import sbt.Keys.Classpath
import sbt.internal.inc.cached.{CacheProvider, CompilationCache}
import xsbti.compile.{CompileAnalysis, MiniSetup}

class LocalCache(sourceDirs: Seq[File], outputDir: File, classpath: Classpath, cacheLocation: File) extends CacheProvider {
  override def findCache(previous: Option[(CompileAnalysis, MiniSetup)]): Option[CompilationCache] = ???
}
