package data

import scala.language.postfixOps

import data.Schemas._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{ ArrayType, MapType, StringType }
import org.apache.spark.sql.{ Dataset, SparkSession }
import utils.{ Commons, Config }

@SuppressWarnings(Array("scalafix:DisableSyntax.null"))
class DataResolver(implicit val spark: SparkSession) {
  import spark.implicits._

  private val reviewsPath = Commons.getDatasetPath("remote", Config.datasetsPath + "/reviews.ndjson")
  private val metadataPath = Commons.getDatasetPath("remote", Config.datasetsPath + "/metadata.ndjson")

  def reviewsDataset: Dataset[Review] = spark.read
    .schema(reviewSchema)
    .json(reviewsPath)
    .withColumn("pics", when(col("pics") isNull, array()) otherwise col("pics"))
    .withColumn("time", from_unixtime(col("time") / 1000).cast("timestamp"))
    .withColumn(
      "resp",
      when(
        col("resp") isNotNull,
        struct(
          from_unixtime(col("resp.time") / 1000).cast("timestamp").alias("time"),
          col("resp.text").cast(StringType).alias("text"),
        ),
      ) otherwise lit(null),
    )
    .as[Review]

  def metadataDataset: Dataset[Metadata] = spark.read
    .schema(metadataSchema)
    .json(metadataPath)
    .withColumn("category", when(col("category") isNull, array()) otherwise col("category"))
    .withColumn("hours", when(col("hours") isNull, array()) otherwise col("hours"))
    .withColumn("relative_results", when(col("relative_results") isNull, array()) otherwise col("relative_results"))
    .withColumn(
      "MISC",
      when(
        col("MISC") isNotNull,
        col("MISC").cast(MapType(StringType, ArrayType(StringType))),
      ) otherwise typedLit(Map.empty[String, Seq[String]]),
    )
    .as[Metadata]

  // Unfortunately, it seems that Spark does not support case classes in RDDs. It throws ArrayStoreException
  // when trying to collect the RDD... [see also [here](https://github.com/adtech-labs/spylon-kernel/issues/40)]
  def reviewsRdd = reviewsDataset.rdd
    .map(Review.unapply)
    .map(_.get)
    .map { case review @ (_, _, _, _, _, _, resp, _) => review.copy(_7 = resp.map(Response.unapply(_).get)) }

  def metadataRdd = metadataDataset.rdd.map(Metadata.unapply).map(_.get)

}
