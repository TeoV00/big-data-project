package data

object ReviewsUtils {

  def toState(address: Option[String]): String = address
    .flatMap { addr =>
      // This regex captures the state abbreviation between a comma and the ZIP code
      val StateRegex = """,\s*([A-Z]{2})\s+\d{5}""".r
      StateRegex.findFirstMatchIn(addr).map(_.group(1))
    }
    .getOrElse("Unknown")

}
