package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.PS
import weaver.*

object PurposeSuite extends FunSuite {

  test("granularize All") {
    expect(
      Purpose.granularize(Purpose.All) == Set(
        "ADVERTISING",
        "COMPLIANCE",
        "EMPLOYMENT",
        "JUSTICE",
        "MARKETING",
        "MEDICAL",
        "PERSONALIZATION",
        "PUBLIC-INTERESTS",
        "RESEARCH",
        "SALE",
        "SECURITY",
        "SERVICES.ADDITIONAL-SERVICES",
        "SERVICES.BASIC-SERVICE",
        "SOCIAL-PROTECTION",
        "TRACKING",
        "VITAL-INTERESTS",
        "OTHER-PURPOSE"
      ).map(Purpose(_))
    )
  }

  test("granularize SERVICES") {
    expect(
      Purpose.granularize(Purpose("SERVICES")) == Set(
        "SERVICES.ADDITIONAL-SERVICES",
        "SERVICES.BASIC-SERVICE"
      ).map(Purpose(_))
    )
  }

  test("granularize ADVERTISING") {
    expect(
      Purpose.granularize(Purpose("ADVERTISING")) == Set(
        "ADVERTISING"
      ).map(Purpose(_))
    )
  }

}
