package org.romanowski.hoarder.amazon

import org.romanowski.hoarder.actions.ci.TravisPRValidation

class S3TravisPRValidation(override val bucketName: String) extends TravisPRValidation with S3Caches
