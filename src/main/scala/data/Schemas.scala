package data

import java.sql.Timestamp

import org.apache.spark.sql.types._

case class Response(time: Timestamp, text: Option[String])

case class Review(
    user_id: Option[String],
    name: Option[String],
    time: Timestamp,
    rating: Option[Double],
    text: Option[String],
    pics: Seq[String],
    resp: Option[Response],
    gmap_id: String,
)

case class Metadata(
    name: Option[String],
    address: Option[String],
    gmap_id: String,
    description: Option[String],
    latitude: Double,
    longitude: Double,
    category: Seq[String],
    avg_rating: Double,
    num_of_reviews: Int,
    price: String,
    hours: Seq[Seq[String]],
    MISC: Map[String, Seq[String]],
    state: Option[String],
    relative_results: Seq[String],
    url: String,
)

object Schemas {

  val reviewSchema: StructType = StructType(
    Seq(
      StructField("user_id", StringType, nullable = true),
      StructField("name", StringType, nullable = true),
      StructField("time", LongType, nullable = false),
      StructField("rating", DoubleType, nullable = true),
      StructField("text", StringType, nullable = true),
      StructField("pics", ArrayType(StringType), nullable = true),
      StructField(
        "resp",
        StructType(
          Seq(
            StructField("time", LongType, nullable = false),
            StructField("text", StringType, nullable = true),
          ),
        ),
        nullable = true,
      ),
      StructField("gmap_id", StringType, nullable = false),
    ),
  )

  val metadataSchema: StructType = StructType(
    Seq(
      StructField("name", StringType, nullable = true),
      StructField("address", StringType, nullable = true),
      StructField("gmap_id", StringType, nullable = false),
      StructField("description", StringType, nullable = true),
      StructField("latitude", DoubleType, nullable = false),
      StructField("longitude", DoubleType, nullable = false),
      StructField("category", ArrayType(StringType), nullable = true),
      StructField("avg_rating", DoubleType, nullable = false),
      StructField("num_of_reviews", IntegerType, nullable = false),
      StructField("price", StringType, nullable = false),
      StructField("hours", ArrayType(ArrayType(StringType)), nullable = true),
      StructField("MISC", MapType(StringType, ArrayType(StringType)), nullable = false),
      StructField("state", StringType, nullable = true),
      StructField("relative_results", ArrayType(StringType), nullable = true),
      StructField("url", StringType, nullable = false),
    ),
  )

}
