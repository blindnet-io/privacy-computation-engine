package io.blindnet.pce
package api.messages

import weaver.*
import io.blindnet.pce.priv.terms.*
import io.blindnet.pce.priv.*
import io.blindnet.pce.api.endpoints.messages.ScopePayload
import io.blindnet.pce.api.endpoints.messages.configuration.*
import testutil.*

object ScopePayloadSuite extends FunSuite {

  val ctx = PSContext(
    selectors = Set(DataCategory("AFFILIATION.s1"), DataCategory("DEVICE.s2"))
  )

  def payload(ts: (Set[String], Set[String], Set[String])*) =
    ts.toSet.map(
      t =>
        ScopePayload(
          t._1.map(DataCategory(_)),
          t._2.map(ProcessingCategory(_)),
          t._3.map(Purpose(_))
        )
    )

  test("full scope") {
    val p = payload((Set("*"), Set("*"), Set("*")))
    expect(ScopePayload.toPrivacyScope(p).zoomIn(ctx) == PrivacyScope.full(ctx))
  }

  test("test 1") {
    val p = payload(
      (
        Set("AFFILIATION"),
        Set("*"),
        Set("RESEARCH")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == scope(
        ("AFFILIATION.MEMBERSHIP.UNION", "ANONYMIZATION", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "AUTOMATED-INFERENCE", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "AUTOMATED-DECISION-MAKING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "GENERATING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "STORING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "SHARING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "USING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "OTHER-PROCESSING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "ANONYMIZATION", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "AUTOMATED-INFERENCE", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "AUTOMATED-DECISION-MAKING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "GENERATING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "STORING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "SHARING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "USING", "RESEARCH"),
        ("AFFILIATION.SCHOOL", "OTHER-PROCESSING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "ANONYMIZATION", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "AUTOMATED-INFERENCE", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "AUTOMATED-DECISION-MAKING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "GENERATING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "STORING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "SHARING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "USING", "RESEARCH"),
        ("AFFILIATION.WORKPLACE", "OTHER-PROCESSING", "RESEARCH"),
        ("AFFILIATION.s1", "ANONYMIZATION", "RESEARCH"),
        ("AFFILIATION.s1", "AUTOMATED-INFERENCE", "RESEARCH"),
        ("AFFILIATION.s1", "AUTOMATED-DECISION-MAKING", "RESEARCH"),
        ("AFFILIATION.s1", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.s1", "GENERATING", "RESEARCH"),
        ("AFFILIATION.s1", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.s1", "STORING", "RESEARCH"),
        ("AFFILIATION.s1", "SHARING", "RESEARCH"),
        ("AFFILIATION.s1", "USING", "RESEARCH"),
        ("AFFILIATION.s1", "OTHER-PROCESSING", "RESEARCH")
      )
    )
  }

  test("test 2") {
    val p = payload(
      (
        Set("AFFILIATION.s1", "AFFILIATION.MEMBERSHIP", "BIOMETRIC"),
        Set("PUBLISHING", "COLLECTION"),
        Set("RESEARCH", "SALE")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == scope(
        ("AFFILIATION.s1", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "PUBLISHING", "RESEARCH"),
        ("BIOMETRIC", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.s1", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "RESEARCH"),
        ("BIOMETRIC", "COLLECTION", "RESEARCH"),
        ("AFFILIATION.s1", "PUBLISHING", "SALE"),
        ("AFFILIATION.MEMBERSHIP.UNION", "PUBLISHING", "SALE"),
        ("BIOMETRIC", "PUBLISHING", "SALE"),
        ("AFFILIATION.s1", "COLLECTION", "SALE"),
        ("AFFILIATION.MEMBERSHIP.UNION", "COLLECTION", "SALE"),
        ("BIOMETRIC", "COLLECTION", "SALE")
      )
    )
  }

  test("test 3") {
    val p = payload(
      (
        Set("AFFILIATION.s1", "BIOMETRIC"),
        Set("PUBLISHING", "COLLECTION"),
        Set("RESEARCH")
      ),
      (
        Set("GENETIC"),
        Set("COLLECTION"),
        Set("SALE")
      ),
      (
        Set("IMAGE"),
        Set("STORING"),
        Set("SERVICES", "TRACKING")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == scope(
        ("AFFILIATION.s1", "PUBLISHING", "RESEARCH"),
        ("AFFILIATION.s1", "COLLECTION", "RESEARCH"),
        ("BIOMETRIC", "PUBLISHING", "RESEARCH"),
        ("BIOMETRIC", "COLLECTION", "RESEARCH"),
        ("GENETIC", "COLLECTION", "SALE"),
        ("IMAGE", "STORING", "SERVICES.ADDITIONAL-SERVICES"),
        ("IMAGE", "STORING", "SERVICES.BASIC-SERVICE"),
        ("IMAGE", "STORING", "TRACKING")
      )
    )
  }

  test("test 4") {
    val p = payload(
      (
        Set("BIOMETRIC"),
        Set("PUBLISHING"),
        Set("RESEARCH")
      ),
      (
        Set("BIOMETRIC", "GENETIC"),
        Set("PUBLISHING"),
        Set("RESEARCH")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == scope(
        ("BIOMETRIC", "PUBLISHING", "RESEARCH"),
        ("GENETIC", "PUBLISHING", "RESEARCH")
      )
    )
  }

  test("test 5") {
    val p = payload(
      (
        Set("DEVICE", "DEVICE.s2"),
        Set("PUBLISHING", "STORING"),
        Set("RESEARCH")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == scope(
        ("DEVICE.s2", "PUBLISHING", "RESEARCH"),
        ("DEVICE.s2", "STORING", "RESEARCH")
      )
    )
  }

  test("test 6") {
    val p = payload(
      (
        Set("*", "DEVICE.s2"),
        Set("*", "STORING"),
        Set("*")
      )
    )
    expect(
      ScopePayload.toPrivacyScope(p).zoomIn(ctx) == PrivacyScope.full(ctx)
    )
  }

}
