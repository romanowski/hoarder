package org.romanowski.hoarder.core

import sbt.Compiler
import sbt.compiler.IC

object SbtTypes {
  type CompilationResult = IC.Result
  type PreviousCompilationResult = Compiler.PreviousAnalysis
}
