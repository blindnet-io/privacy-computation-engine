package io.blindnet.pce
package priv

import java.time.Instant
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
import weaver.*

object PrivacyScopeSuite extends FunSuite {

  object fixtures {
    val ctx = PSContext(
      Set("CONTACT.s1", "CONTACT.s2", "CONTACT.EMAIL.s3").map(DataCategory(_))
    )

  }

  import fixtures.*

  test("empty PS to most granular categories") {
    val ps = PrivacyScope.empty.zoomIn()
    expect(ps == PrivacyScope.empty)
  }

  test("full scope to most granular categories") {
    val ps = scope(("*", "*", "*")).zoomIn()
    expect(ps == PrivacyScope.full())
  }

  test("simple scope to most granular categories 1") {
    val ps = scope(("CONTACT", "ANONYMIZATION", "ADVERTISING")).zoomIn()
    expect(
      ps == scope(
        ("CONTACT.EMAIL", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.ADDRESS", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.PHONE", "ANONYMIZATION", "ADVERTISING")
      )
    )
  }

  test("simple scope to most granular categories 2") {
    val ps = scope(("CONTACT", "ANONYMIZATION", "SERVICES")).zoomIn()

    expect(
      ps == scope(
        ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
        ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
        ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.BASIC-SERVICE"),
        ("CONTACT.EMAIL", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
        ("CONTACT.ADDRESS", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES"),
        ("CONTACT.PHONE", "ANONYMIZATION", "SERVICES.ADDITIONAL-SERVICES")
      )
    )
  }

  test("simple scope to most granular categories 3") {
    val ps = scope(
      ("CONTACT", "ANONYMIZATION", "SERVICES"),
      ("AFFILIATION", "COLLECTION", "ADVERTISING")
    ).zoomIn()

    expect(
      ps == scope(
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
    )
  }

  test("empty scope with context containing selectors to most granular categories") {
    val ps = PrivacyScope.empty.zoomIn(ctx)
    expect(ps == PrivacyScope.empty)
  }

  test("scope with context containing selectors to most granular categories 1") {
    val ps = scope(("CONTACT", "ANONYMIZATION", "ADVERTISING")).zoomIn(ctx)
    expect(
      ps == scope(
        ("CONTACT.ADDRESS", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.PHONE", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.s1", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.s2", "ANONYMIZATION", "ADVERTISING"),
        ("CONTACT.EMAIL.s3", "ANONYMIZATION", "ADVERTISING")
      )
    )
  }

  test("scope with context containing selectors to most granular categories 2") {
    val ps = scope(
      ("CONTACT", "ANONYMIZATION", "ADVERTISING"),
      ("AFFILIATION", "COLLECTION", "SERVICES")
    ).zoomIn(ctx)

    expect(
      ps == scope(
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
    )
  }

}
