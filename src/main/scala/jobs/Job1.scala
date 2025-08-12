package jobs

object Job1 {

  def main(args: Array[String]): Unit = args match {
    case Array("basic") => basic()
    case Array("optimized") => optimized()
    case _ => sys.error("""
        |Wrong parameters.
        |
        |Usage: Job1 <mode>
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
    implicit val spark: SparkSession = SparkSession.builder.appName("Job1 Basic").getOrCreate()
    val data = new DataResolver()
    import spark.sqlContext.implicits._ // needed to save as CSV, see `toDF` method

    val businessesStates = data.metadataRdd
      .filter(_._2.isDefined) // leave only businesses with a defined address
      .map(b => b._3 -> toState(b._2))

    val reviewsInfo = data.reviewsRdd
      .filter(_._4.isDefined) // filter out reviews without a rating
      .map { case (_, _, time, rating, _, _, resp, id) => id -> (time, rating.get, resp) }
      .join(businessesStates) // [(gmap_id, ((timestamp, rating, response), state))*]
      .map { case (id, ((time, rating, resp), state)) =>
        (time.toLocalDateTime.getYear, id, state) -> (time, rating, resp)
      }
      .aggregateByKey((0.0, 0, 0L, 0))( // (ratings sum, #responses, sum of response time differences, #reviews)
        (acc, v) => {
          val (sumRatings, numResponses, sumResponseTimes, totalReviews) = acc
          val (time, rating, response) = v
          (
            sumRatings + rating,
            numResponses + (if (response.isDefined) 1 else 0),
            sumResponseTimes + (if (response.isDefined) response.get._1.getTime - time.getTime else 0L),
            totalReviews + 1,
          )
        },
        (r1, r2) => (r1._1 + r2._1, r1._2 + r2._2, r1._3 + r2._3, r1._4 + r2._4),
      )
      .mapValues { case (sumRatings, numResponses, sumResponseTimes, totalReviews) =>
        (
          sumRatings / totalReviews,
          numResponses.toDouble / totalReviews,
          if (numResponses > 0) TimeUnit.MILLISECONDS.toHours(sumResponseTimes / numResponses)
          else Double.PositiveInfinity,
        )
      } // [((year, gmap_id, state), (avg_rating, response_rate, avg_response_time))*]
      .mapValues { case (avgRating, responseRate, avgResponseTime) =>
        (avgRating, responseRate, avgResponseTime, responseStrategy(responseRate, avgResponseTime))
      } // [((year, gmap_id, state), (avg_rating, response_rate, avg_response_time, response_strategy))*]

    val outcome = reviewsInfo
      .map { case ((year, _, state), (avgRating, _, _, responseStrategy)) =>
        (year, state, responseStrategy) -> avgRating
      }
      .aggregateByKey((0.0, 0))((acc, v) => (acc._1 + v, acc._2 + 1), (r1, r2) => (r1._1 + r2._1, r1._2 + r2._2))
      .mapValues { case (sumRatings, totalBusinesses) => sumRatings / totalBusinesses }

    outcome
      .map { case ((year, state, responseStrategy), avgRating) => (year, state, responseStrategy, avgRating) }
      .coalesce(1)
      .toDF("year", "state", "response_strategy", "avg_rating")
      .write
      .format("csv")
      .option("header", "true")
      .mode(SaveMode.Overwrite)
      .save(Commons.getDatasetPath("remote", s"${Config.outputDirPath}/job1-output"))
  }

  private def optimized(): Unit = {
    implicit val spark: SparkSession = SparkSession.builder.appName("Job1 Optimized").getOrCreate()
    val data = new DataResolver()
    import spark.sqlContext.implicits._ // needed to save as CSV, see `toDF` method

    val partitions = 24
    val partitioner = new org.apache.spark.HashPartitioner(partitions)

    val businessesStates = data.metadataRdd
      .filter(_._2.isDefined) // leave only businesses with a defined address
      .map(b => b._3 -> toState(b._2)) // [(gmap_id, state)*]
      .partitionBy(partitioner)

    val reviewsInfo = data.reviewsRdd
      .filter(_._4.isDefined) // filter out reviews without a rating
      .map { case (_, _, time, rating, _, _, resp, id) =>
        (time.toLocalDateTime.getYear, id) -> (time, rating.get, resp.map(_._1))
      }
      .aggregateByKey((0.0, 0, 0L, 0))( // (ratings sum, #responses, sum of response time differences, #reviews)
        (acc, v) => {
          val (sumRatings, numResponses, sumResponseTimes, totalReviews) = acc
          val (time, rating, response) = v
          (
            sumRatings + rating,
            numResponses + (if (response.isDefined) 1 else 0),
            sumResponseTimes + (if (response.isDefined) response.get.getTime - time.getTime else 0L),
            totalReviews + 1,
          )
        },
        (r1, r2) => (r1._1 + r2._1, r1._2 + r2._2, r1._3 + r2._3, r1._4 + r2._4),
      )
      .mapValues { case (sumRatings, numResponses, sumResponseTimes, totalReviews) =>
        val avgResponseRate = numResponses.toDouble / totalReviews
        val avgResponseTime =
          if (numResponses > 0) TimeUnit.MILLISECONDS.toHours(sumResponseTimes / numResponses)
          else Double.PositiveInfinity
        (
          sumRatings / totalReviews,
          avgResponseRate,
          avgResponseTime,
          responseStrategy(avgResponseRate, avgResponseTime),
        )
      } // [((year, gmap_id), (avg_rating, response_rate, avg_response_time, response_strategy))*]
      .coalesce(partitions)

    val outcome = reviewsInfo
      .map { case ((year, id), (avgRating, _, _, responseStrategy)) => id -> (year, responseStrategy, avgRating) }
      .partitionBy(partitioner)
      .join(businessesStates) // [(gmap_id, ((year, response_strategy, avg_rating), state))*]
      .map { case (_, ((year, responseStrategy, avgRating), state)) =>
        (year, state, responseStrategy) -> (avgRating, 1)
      }
      .reduceByKey((a, b) => (a._1 + b._1, a._2 + b._2))
      .mapValues { case (sumRatings, totalBusinesses) => sumRatings / totalBusinesses }

    outcome
      .map { case ((year, state, responseStrategy), avgRating) => (year, state, responseStrategy, avgRating) }
      .coalesce(1)
      .toDF("year", "state", "response_strategy", "avg_rating")
      .write
      .format("csv")
      .option("header", "true")
      .mode(SaveMode.Overwrite)
      .save(Commons.getDatasetPath("remote", s"${Config.outputDirPath}/job1-optimized-output"))
  }

  private def responseStrategy(avgResponseRate: Double, avgResponseTime: Double): String =
    (avgResponseRate, avgResponseTime) match {
      case (rr, rt) if rr >= 0.5 && rt <= 4 * 24 => "Rapid and frequent"
      case (rr, _) if rr >= 0.5 => "Slow but frequent"
      case (rr, _) if rr >= 0.15 => "Occasional"
      case _ => "Rare or none"
    }
}
