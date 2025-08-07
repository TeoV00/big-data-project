package utils

import java.io.File

object Config {

  /** The local directory containing this repository. */
  val projectDir: File = new File(System.getProperty("user.dir"))

  /** The directory containing the datasets. */
  val datasetsPath: String = "/dataset"

  val outputDirPath: String = "/output"

  /** The name of the bucket on AWS S3 */
  val s3bucketName: String = "google-local-reviews-analysis"

  /** The path to the credentials file for AWS. */
  val credentialsPath: String = "/aws_credentials.txt"

}
