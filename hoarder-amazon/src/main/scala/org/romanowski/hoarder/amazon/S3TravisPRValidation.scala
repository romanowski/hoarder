package org.romanowski.hoarder.amazon

import org.romanowski.hoarder.actions.CachedCI
import org.romanowski.hoarder.actions.ci.TravisFlow

case class S3TravisPRValidation(override val bucketName: String,
                                override val prefix: String = "") extends TravisFlow with S3Caches

case class S3SimpleFlow(override val bucketName: String) extends CachedCI.Setup with S3Caches {
  override def shouldPublishCaches(): Boolean = true

  override def shouldUseCache(): Boolean = true

  override def prefix = "caches"
}