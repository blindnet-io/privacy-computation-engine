package io.blindnet.pce
package priv

import java.time.Instant
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.funspec.*
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.PS

class PurposeSpec extends UnitSpec {

  describe("Purpose") {
    describe("should resolve to most granular categories") {
      it("for All category") {
        Purpose.granularize(Purpose.All) shouldBe Set(
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
      }

      it("for category with subcategories") {
        Purpose.granularize(Purpose("SERVICES")) shouldBe Set(
          "SERVICES.ADDITIONAL-SERVICES",
          "SERVICES.BASIC-SERVICE"
        ).map(Purpose(_))
      }

      it("for lowest level category") {
        Purpose.granularize(Purpose("ADVERTISING")) shouldBe Set(
          "ADVERTISING"
        ).map(Purpose(_))
      }

    }
  }

}
