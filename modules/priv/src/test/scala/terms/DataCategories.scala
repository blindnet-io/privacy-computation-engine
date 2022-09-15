package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import weaver.*

object DataCategorySuite extends FunSuite {

  object fixtures {
    val ctx = PSContext(
      Set("DEVICE.PHONE", "CONTACT.s2", "CONTACT.EMAIL.s3").map(DataCategory(_))
    )

  }

  import fixtures.*

  test("granularize All") {
    expect(
      DataCategory.granularize(DataCategory.All) == Set(
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
    )
  }

  test("granularize AFFILIATION") {
    expect(
      DataCategory.granularize(DataCategory("AFFILIATION")) == Set(
        "AFFILIATION.MEMBERSHIP.UNION",
        "AFFILIATION.SCHOOL",
        "AFFILIATION.WORKPLACE"
      ).map(DataCategory(_))
    )
  }

  test("granularize AFFILIATION.MEMBERSHIP.UNION") {
    expect(
      DataCategory.granularize(DataCategory("AFFILIATION.MEMBERSHIP.UNION")) == Set(
        "AFFILIATION.MEMBERSHIP.UNION"
      ).map(DataCategory(_))
    )
  }

  test("granularize All with context selectors") {
    expect(
      DataCategory.granularize(DataCategory.All, ctx.selectors) == Set(
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
    )
  }

  test("granularize CONTACT with context selectors") {
    expect(
      DataCategory.granularize(DataCategory("CONTACT"), ctx.selectors) == Set(
        "CONTACT.s2",
        "CONTACT.EMAIL.s3",
        "CONTACT.ADDRESS",
        "CONTACT.PHONE"
      ).map(DataCategory(_))
    )
  }

  test("granularize selector CONTACT.EMAIL.s3 from context") {
    expect(
      DataCategory.granularize(DataCategory("CONTACT.EMAIL.s3"), ctx.selectors) == Set(
        "CONTACT.EMAIL.s3"
      ).map(DataCategory(_))
    )
  }

}
