package org.romanowski.hoarder.amazon

import java.nio.file.Path

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.romanowski.hoarder.actions.ci.DownloadableCacheSetup

trait S3Caches extends DownloadableCacheSetup {

  def bucketName: String

  def prefix: String

  def dirLikePrefix = if (prefix.endsWith("/")) prefix else s"$prefix/"

  protected def s3ClientBuilder = AmazonS3ClientBuilder.standard()

  protected def transferManagerBuilder = TransferManagerBuilder.standard().withS3Client(s3ClientBuilder.build())

  /** Loads cache to temporary directory */
  override def downloadCache(to: Path): Path = {
    transferManagerBuilder.build()
      .downloadDirectory(bucketName, dirLikePrefix, to.toFile)
      .waitForCompletion()
    to.resolve(dirLikePrefix)
  }


  /** Stores cache from temporary directory */
  override def uploadCache(from: Path): Unit = transferManagerBuilder.build()
    .uploadDirectory(bucketName, dirLikePrefix, from.toFile, /*includeSubdirectories =*/ true)
    .waitForCompletion()

  /** Remove current cache entry for this job. Called before new cache is exported */
  override def invalidateCache(cachePrefix: String): Unit = {
    import collection.JavaConverters._
    val s3 = s3ClientBuilder.build()
    val objects = s3.listObjects(bucketName, s"$dirLikePrefix$cachePrefix").getObjectSummaries.asScala
    objects.foreach { summary =>
          s3.deleteObject(bucketName, summary.getKey)
      }
  } // We don't need to invalidate since upload will override old entries
}
