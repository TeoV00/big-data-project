package data

object ReviewsUtils {

  /** This regex captures the state abbreviation between a comma and the ZIP code. */
  private val StateRegex = """,\s*([A-Z]{2})\s+\d{5}""".r

  /** The map of states that are considered for the analysis. */
  val consideredStates = Map(
    "Alabama" -> "AL",
    "Mississippi" -> "MS",
    "New Hampshire" -> "NH",
    "New Mexico" -> "NM",
    "Washington" -> "WA",
  )

  /** Regex to match state names in the address. */
  private val StateNameRegex = s"""\\b(${consideredStates.keys.mkString("|")})\\b""".r

  /** Regex to match state abbreviations in the address. */
  private val StateAbbrevRegex = s"""\\b(${consideredStates.values.mkString("|")})\\b""".r

  /**
   * Extracts the state from the given address.
   * @param address
   *   the optional address string
   * @return
   *   the state abbreviation or "Unknown" if no valid state is found
   * @see
   *   [[consideredStates]]
   */
  def toState(address: Option[String]): String = address
    .flatMap { addr =>
      StateRegex
        .findFirstMatchIn(addr)
        .map(_.group(1))
        .orElse(StateNameRegex.findFirstMatchIn(addr).map(stateName => consideredStates(stateName.group(1))))
        .orElse(StateAbbrevRegex.findFirstMatchIn(addr).map(_.group(1)))
    }
    .filter(consideredStates.values.toSeq.contains)
    .getOrElse("Unknown")

}
