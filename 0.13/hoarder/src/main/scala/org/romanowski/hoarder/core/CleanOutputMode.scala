/*
 * Hoarder - Cached compilation plugin for sbt.
 * Copyright 2016 - 2017, Krzysztof Romanowski
 * This software is released under the terms written in LICENSE.
 */

package org.romanowski.hoarder.core

sealed trait CleanOutputMode

case object CleanOutput extends CleanOutputMode

case object FailOnNonEmpty extends CleanOutputMode

case object CleanClasses extends CleanOutputMode