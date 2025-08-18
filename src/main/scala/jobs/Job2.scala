package jobs

object Job2 {

  def main(args: Array[String]): Unit = args match {
    case Array("basic") => basic()
    case Array("optimized") => optimized()
    case _ => sys.error("""
        |Wrong parameters.
        |
        |Usage: Job2 <mode>
        |  where <mode> can be:
        |    basic       - runs the non-optimized job
        |    optimized   - runs the optimized version of the job
        |""".stripMargin)
  }

  import org.apache.spark.sql.{ SaveMode, SparkSession }
  import data.DataResolver
  import data.ReviewsUtils._
  import java.util.concurrent.TimeUnit
  import utils.{ Commons, Config }

  private def basic(): Unit = {
    implicit val spark: SparkSession = SparkSession.builder.appName("Job2 Basic").getOrCreate()
    val data = new DataResolver()
    import spark.sqlContext.implicits._ // needed to save as CSV, see `toDF` method

    val businessAvgRating = data.reviewsRdd
      .filter(_._4.isDefined)
      .map { case r =>
        val rating = r._4.get
        val gmap_id = r._8
        val year = r._3.toLocalDateTime.getYear
        (gmap_id, year) -> (1, rating)
      }
      .reduceByKey((a, b) => (a._1 + b._1, a._2 + b._2))
      .map { case ((gmap_id, year), c) => gmap_id -> (year, c._2 / c._1) }

    val results = data.metadataRdd
      .filter(r => Option(r._10).exists(_.nonEmpty))
      .flatMap(r => r._7.map(category => r._3 -> (category, toState(r._2), r._10)))
      .join(businessAvgRating)
      .map { case (_, ((category, state, price), (year, avgRate))) =>
        (category, state, price, year) -> avgRate
      }
      .groupByKey()
      .map { case (key, ratings) =>
        val avgRate = ratings.sum / ratings.size
        (key, f"${avgRate}%.2f", ratingToSuggestion(avgRate))
      }

    results
      .map { case ((category, state, price, year), avgRate, suggestion) =>
        (category, state, price, year, avgRate, suggestion)
      }
      .coalesce(1)
      .toDF("category", "state", "price", "year", "avg_rating", "business_suggestion")
      .write
      .format("csv")
      .option("header", "true")
      .mode(SaveMode.Overwrite)
      .save(Commons.getDatasetPath("remote", s"${Config.outputDirPath}/job2-output"))
  }

  private def optimized(): Unit = {
    implicit val spark: SparkSession = SparkSession.builder.appName("Job2 Optimized").getOrCreate()
    val data = new DataResolver()
    import spark.sqlContext.implicits._ // needed to save as CSV, see `toDF` method

    //   outcome.map { case ((year, state, responseStrategy), avgRating) => (year, state, responseStrategy, avgRating) }
    //     .coalesce(1, shuffle = true)
    //     .toDF("category", "state", "price", "year", "avg_rating", "business suggestion")
    //     .write
    //     .format("csv")
    //     .option("header", "true")
    //     .mode(SaveMode.Overwrite)
    //     .save(Commons.getDatasetPath("remote", s"${Config.outputDirPath}/job2-optimized-output"))
  }

  def ratingToSuggestion(rating: Double): String =
    rating match {
      case r if r <= 2.0 => "Not recommended"
      case r if r > 2.0 && r <= 3.5 => "Discreet"
      case r if r > 3.5 && r <= 4.5 => "Recommended"
      case r if r > 4.5 => "Highly recommended"
      case _ => "Undefined"
    }

}
