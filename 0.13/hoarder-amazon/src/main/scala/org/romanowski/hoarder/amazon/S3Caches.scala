package org.romanowski.hoarder.amazon

import java.nio.file.Path

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.romanowski.hoarder.actions.ci.DownloadableCacheSetup

trait S3Caches extends DownloadableCacheSetup {

  def bucketName: String

  protected def s3ClientBuilder = AmazonS3ClientBuilder.standard()

  protected def transferManagerBuilder = TransferManagerBuilder.standard().withS3Client(s3ClientBuilder.build())

  /** Loads cache to temporary directory */
  override def downloadCache(to: Path): Unit = transferManagerBuilder.build()
    .downloadDirectory(bucketName, "", to.toFile)
    .waitForCompletion()


  /** Stores cache from temporary directory */
  override def uploadCache(from: Path): Unit = transferManagerBuilder.build()
    .uploadDirectory(bucketName, "", from.toFile, /*includeSubdirectories =*/ true)
    .waitForCompletion()
}
