package utils

object Config {

  /** The local directory containing this repository. */
  val projectDir: String = System.getProperty("user.dir")

  /** The name of the shared bucket on AWS S3 to read datasets. */
  val s3sharedBucketName: String = "unibo-bd2425-egallinucci"

  /** The name of the bucket on AWS S3 */
  val s3bucketName: String = "google-local-reviews-analysis"

  /** The path to the credentials file for AWS. */
  val credentialsPath: String = "/aws_credentials.txt"

}
