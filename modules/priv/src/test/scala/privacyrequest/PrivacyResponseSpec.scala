package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.privacyrequest.*
import cats.implicits.*
import cats.effect.IO
import weaver.*

object PrivacyResponseSuite extends SimpleIOSuite {

  object fixtures {}

  import fixtures.*
  import io.blindnet.pce.priv.test.*

  import Action.*

  test("response from request with Access demand") {
    val pr = request(List(demand(Access)))
    PrivacyResponse
      .fromPrivacyRequest[IO](pr)
      .map(
        resp => {
          val r = resp.head
          expect(r.demandId == pr.demands.head.id) and
            expect(r.timestamp == pr.timestamp) and
            expect(r.action == Access) and
            expect(r.status == Status.UnderReview) and
            expect(r.parent == None) and
            expect(r.includes == List.empty)
        }
      )
  }

  test("response from request with Transparency demand") {
    val pr = request(List(demand(Transparency)))
    PrivacyResponse
      .fromPrivacyRequest[IO](pr)
      .map(
        resp => {
          val r = resp.head
          expect(r.demandId == pr.demands.head.id) and
            expect(r.timestamp == pr.timestamp) and
            expect(r.action == Transparency) and
            expect(r.status == Status.UnderReview) and
            expect(r.parent == None) and
            expect(
              r.includes.map(_.action).sortBy(x => x.encode) == List(
                TDataCategories,
                TDPO,
                TKnown,
                TLegalBases,
                TOrganization,
                TPolicy,
                TProcessingCategories,
                TProvenance,
                TPurpose,
                TRetention,
                TWhere,
                TWho
              ).sortBy(x => x.encode)
            )
        }
      )
  }

  test("response from request with multiple demands") {
    val pr = request(List(demand(Access), demand(Delete), demand(RevokeConsent)))
    PrivacyResponse
      .fromPrivacyRequest[IO](pr)
      .map(resp => expect(resp.length == 3))
  }

  pureTest("group unrelated responses") {
    val r1 = response(a = Access)
    val r2 = response(a = Delete)
    val r3 = response(a = Transparency)

    expect(PrivacyResponse.group(List(r1, r2, r3)) == List(r1, r2, r3))
  }

  pureTest("group related responses") {
    val r1   = response(a = Access)
    val r1a  = response(a = Access, parent = r1.id.some)
    val r1b  = response(a = Access, parent = r1.id.some)
    val r1a1 = response(a = Modify, parent = r1a.id.some)
    val r1a2 = response(a = Access, parent = r1a.id.some)
    val r2   = response(a = Transparency)
    val r2a  = response(a = TWho, parent = r2.id.some)
    val r3   = response(a = Delete)

    val test = PrivacyResponse.group(List(r1, r1a, r1b, r1a1, r1a2, r2, r2a, r3))

    expect(
      test == List(
        r1.copy(includes =
          List(
            r1a.copy(includes = List(r1a1, r1a2)),
            r1b
          )
        ),
        r2.copy(includes = List(r2a)),
        r3
      )
    )
  }

}
