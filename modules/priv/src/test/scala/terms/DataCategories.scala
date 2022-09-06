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
import io.blindnet.pce.priv.terms.DataCategory
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.PS

class DataCategorySpec extends UnitSpec {

  object fixtures {
    val ctx = PSContext(
      Set("DEVICE.PHONE", "CONTACT.s2", "CONTACT.EMAIL.s3").map(DataCategory(_))
    )

  }

  import fixtures.*

  describe("DataCategory") {
    describe("should resolve to most granular categories") {
      it("for All category") {
        DataCategory.granularize(DataCategory.All) shouldBe Set(
          "AFFILIATION.MEMBERSHIP.UNION",
          "AFFILIATION.SCHOOL",
          "AFFILIATION.WORKPLACE",
          "BEHAVIOR.ACTIVITY",
          "BEHAVIOR.CONNECTION",
          "BEHAVIOR.PREFERENCE",
          "BEHAVIOR.TELEMETRY",
          "BIOMETRIC",
          "CONTACT.EMAIL",
          "CONTACT.ADDRESS",
          "CONTACT.PHONE",
          "DEMOGRAPHIC.AGE",
          "DEMOGRAPHIC.BELIEFS",
          "DEMOGRAPHIC.GENDER",
          "DEMOGRAPHIC.ORIGIN",
          "DEMOGRAPHIC.RACE",
          "DEMOGRAPHIC.SEXUAL-ORIENTATION",
          "DEVICE",
          "FINANCIAL.BANK-ACCOUNT",
          "GENETIC",
          "HEALTH",
          "IMAGE",
          "LOCATION",
          "NAME",
          "PROFILING",
          "RELATIONSHIPS",
          "UID.ID",
          "UID.IP",
          "UID.USER-ACCOUNT",
          "UID.SOCIAL-MEDIA",
          "OTHER-DATA"
        ).map(DataCategory(_))
      }

      it("for category with subcategories") {
        DataCategory.granularize(DataCategory("AFFILIATION")) shouldBe Set(
          "AFFILIATION.MEMBERSHIP.UNION",
          "AFFILIATION.SCHOOL",
          "AFFILIATION.WORKPLACE"
        ).map(DataCategory(_))
      }

      it("for lowest level category") {
        DataCategory.granularize(DataCategory("AFFILIATION.MEMBERSHIP.UNION")) shouldBe Set(
          "AFFILIATION.MEMBERSHIP.UNION"
        ).map(DataCategory(_))
      }

      it("for All category and selector context") {
        DataCategory.granularize(DataCategory.All, ctx.selectors) shouldBe Set(
          "AFFILIATION.MEMBERSHIP.UNION",
          "AFFILIATION.SCHOOL",
          "AFFILIATION.WORKPLACE",
          "BEHAVIOR.ACTIVITY",
          "BEHAVIOR.CONNECTION",
          "BEHAVIOR.PREFERENCE",
          "BEHAVIOR.TELEMETRY",
          "BIOMETRIC",
          "CONTACT.s2",
          "CONTACT.EMAIL.s3",
          "CONTACT.ADDRESS",
          "CONTACT.PHONE",
          "DEMOGRAPHIC.AGE",
          "DEMOGRAPHIC.BELIEFS",
          "DEMOGRAPHIC.GENDER",
          "DEMOGRAPHIC.ORIGIN",
          "DEMOGRAPHIC.RACE",
          "DEMOGRAPHIC.SEXUAL-ORIENTATION",
          "DEVICE.PHONE",
          "FINANCIAL.BANK-ACCOUNT",
          "GENETIC",
          "HEALTH",
          "IMAGE",
          "LOCATION",
          "NAME",
          "PROFILING",
          "RELATIONSHIPS",
          "UID.ID",
          "UID.IP",
          "UID.USER-ACCOUNT",
          "UID.SOCIAL-MEDIA",
          "OTHER-DATA"
        ).map(DataCategory(_))
      }

      it("for category with subcategories and selector context") {
        DataCategory.granularize(DataCategory("CONTACT"), ctx.selectors) shouldBe Set(
          "CONTACT.s2",
          "CONTACT.EMAIL.s3",
          "CONTACT.ADDRESS",
          "CONTACT.PHONE"
        ).map(DataCategory(_))
      }

      it("for lowest level category and selector context") {
        DataCategory.granularize(DataCategory("CONTACT.EMAIL.s3"), ctx.selectors) shouldBe Set(
          "CONTACT.EMAIL.s3"
        ).map(DataCategory(_))
      }

    }
  }

}
