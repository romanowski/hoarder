package org.romanowski.hoarder.tests

import java.io.File

import org.romanowski.hoarder.actions.CachedCI
import org.romanowski.hoarder.amazon.S3Caches

class S3TestIntegrationSetup extends CachedCI.Setup with S3Caches {
  override def bucketName: String = sys.env.getOrElse("HOARDER_BUCKET_NAME", "hoarder-s3-scripted-test")

  override def prefix: String = sys.env.getOrElse("HOARDER_BUCKET_PREFIX", "default/")

  override def shouldPublishCaches(): Boolean = new File(".shouldPublishCaches").exists()

  override def shouldUseCache(): Boolean = new File(".shouldUseCache").exists()
}

class S3TestPlugin(setup: CachedCI.Setup = new S3TestIntegrationSetup) extends CachedCI.PluginBase(setup)