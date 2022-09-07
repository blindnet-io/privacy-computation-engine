package io.blindnet.pce
package priv

import java.time.Instant
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.funspec.*
import io.blindnet.pce.priv.terms.EventTerms
import io.blindnet.pce.priv.terms.LegalBaseTerms
import io.blindnet.pce.priv.terms.DataCategory
import scala.util.Random
import io.blindnet.pce.priv.terms.ProcessingCategory
import io.blindnet.pce.priv.terms.Purpose
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.test.*

class PrivacyScopeSpec extends UnitSpec {

  object fixtures {
    val ctx = PSContext(
      Set("CONTACT.s1", "CONTACT.s2", "CONTACT.EMAIL.s3").map(DataCategory(_))
    )

  }

  import fixtures.*

  describe("Privacy scope") {
    describe("should resolve to most granular categories") {
      it("for empty scope") {
        val ps = PrivacyScope.empty.zoomIn()
        ps shouldBe PrivacyScope.empty
      }

      it("for full scope") {
        val ps = scope(("*", "*", "*")).zoomIn()
        ps shouldBe fullPS
      }

      it("for simple scope 1") {
        val ps = scope(("CONTACT", "ANONYMIZATION", "ADVERTISING")).zoomIn()
        ps shouldBe scope(
          ("CONTACT.EMAIL", "ANONYMIZATION", "ADVERTISING"),
          ("CONTACT.ADDRESS", "ANONYMIZATION", "ADVERTISING"),
          ("CONTACT.PHONE", "ANONYMIZATION", "ADVERTISING")
        )
      }

      it("for simple scope 2") {
        val ps = scope(("CONTACT", "ANONYMIZATION", "SERVICES")).zoomIn()

        ps shouldBe scope(
          ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
          ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
          ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES")
        )
      }

      it("for simple scope 3") {
        val ps = scope(
          ("CONTACT", "ANONYMIZATION", "SERVICES"),
          ("AFFILIATION", "COLLECTION", "ADVERTISING")
        ).zoomIn()

        ps shouldBe scope(
          ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
          ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
          ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
          ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
          ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "ADVERTISING"),
          ("AFFILIATION.SCHOOL", "COLLECTION", "ADVERTISING"),
          ("AFFILIATION.WORKPLACE", "COLLECTION", "ADVERTISING")
        )
      }
    }

    describe("with context containing selectors") {
      describe("should resolve to most granular categories") {
        it("for empty scope") {
          val ps = PrivacyScope.empty.zoomIn(ctx)
          ps shouldBe PrivacyScope.empty
        }

        it("for simple scope 1") {
          val ps = scope(("CONTACT", "ANONYMIZATION", "ADVERTISING")).zoomIn(ctx)
          ps shouldBe scope(
            ("CONTACT.ADDRESS", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.PHONE", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.s1", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.s2", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.EMAIL.s3", "ANONYMIZATION", "ADVERTISING")
          )
        }

        it("for simple scope 2") {
          val ps = scope(
            ("CONTACT", "ANONYMIZATION", "ADVERTISING"),
            ("AFFILIATION", "COLLECTION", "SERVICES")
          ).zoomIn(ctx)

          ps shouldBe scope(
            ("CONTACT.ADDRESS", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.PHONE", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.s1", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.s2", "ANONYMIZATION", "ADVERTISING"),
            ("CONTACT.EMAIL.s3", "ANONYMIZATION", "ADVERTISING"),
            ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "SERVICES.BASIC-SERVICE"),
            ("AFFILIATION.SCHOOL", "COLLECTION", "SERVICES.BASIC-SERVICE"),
            ("AFFILIATION.WORKPLACE", "COLLECTION", "SERVICES.BASIC-SERVICE"),
            ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "SERVICES.ADDITIONAL-SERVICES"),
            ("AFFILIATION.SCHOOL", "COLLECTION", "SERVICES.ADDITIONAL-SERVICES"),
            ("AFFILIATION.WORKPLACE", "COLLECTION", "SERVICES.ADDITIONAL-SERVICES")
          )
        }
      }
    }
  }

}
