package org.romanowski

sealed trait CleanOutputMode

case object CleanOutput extends CleanOutputMode

case object FailOnNonEmpty extends CleanOutputMode

case object CleanClasses extends CleanOutputMode