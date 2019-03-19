package org.romanowski.hoarder.amazon

import org.romanowski.hoarder.actions.CachedCI

object HoarderS3 {
  class TravisPR(bucketName: String, prefix: => String = sys.env.getOrElse("TRAVIS_BRANCH", "default")) extends
    CachedCI.PluginBase(S3TravisPRValidation(bucketName, prefix))

  class Simple(bucketName: String) extends CachedCI.PluginBase(S3SimpleFlow(bucketName))
}
