package org.romanowski.hoarder.amazon

import org.romanowski.hoarder.actions.ci.TravisFlow

case class S3TravisPRValidation(override val bucketName: String,
                                override val prefix: String = "") extends TravisFlow with S3Caches