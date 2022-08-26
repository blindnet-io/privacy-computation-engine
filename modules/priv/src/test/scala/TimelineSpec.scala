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

class TimelineSpec extends UnitSpec {

  object fixtures {

    extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

    extension (i: Instant) def -(n: Int) = i.minus(1, ChronoUnit.DAYS)

    def uuid = java.util.UUID.randomUUID

    def scope(t: (String, String, String)*) = PrivacyScope(
      t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
    )

    def randomScope() = scope(
      (1 to Random.nextInt(10))
        .map(
          _ => (DataCategory.terms.sample, ProcessingCategory.terms.sample, Purpose.terms.sample)
        )*
    )

    val scope1 = scope(
      ("AFFILIATION.MEMBERSHIP", "ANONYMIZATION", "ADVERTISING"),
      ("CONTACT.EMAIL", "COLLECTION", "ADVERTISING")
    )

    val scope2 = scope(
      ("AFFILIATION.MEMBERSHIP", "ANONYMIZATION", "ADVERTISING"),
      ("DEMOGRAPHIC.AGE", "STORING", "MEDICAL"),
      ("DEMOGRAPHIC.AGE", "COLLECTION", "TRACKING")
    )

    val now = Instant.now()

    val emptyS = PrivacyScope.empty

    def lbEvent(
        id: UUID,
        typ: EventTerms,
        lb: LegalBaseTerms,
        s: PrivacyScope,
        t: Instant = now
    ) =
      TimelineEvent.LegalBase(id, typ, lb, t, s)

    def cgEvent(id: UUID, s: PrivacyScope, t: Instant = now) =
      TimelineEvent.ConsentGiven(id, t, s)

    def crEvent(id: UUID, t: Instant = now) =
      TimelineEvent.ConsentRevoked(id, t)

    def startServiceContract(id: UUID, s: PrivacyScope, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceStart, LegalBaseTerms.Contract, s, t)

    def endServiceContract(id: UUID, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceEnd, LegalBaseTerms.Contract, PrivacyScope.empty, t)

    def startServiceNecessary(id: UUID, s: PrivacyScope, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceStart, LegalBaseTerms.Necessary, s, t)

    def endServiceNecessary(id: UUID, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceEnd, LegalBaseTerms.Necessary, PrivacyScope.empty, t)

    def startServiceLegit(id: UUID, s: PrivacyScope, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceStart, LegalBaseTerms.LegitimateInterest, s, t)

    def endServiceLegit(id: UUID, t: Instant = now) =
      lbEvent(id, EventTerms.ServiceEnd, LegalBaseTerms.LegitimateInterest, PrivacyScope.empty, t)

  }

  import fixtures.*

  describe("A Timeline") {
    describe("with no events") {
      it("should return an empty privacy scope") {
        Timeline.empty.eligiblePrivacyScope() shouldBe PrivacyScope.empty
      }
    }

    describe("should calculate a correct privacy scope") {
      it("for 2 contract start events") {
        val uuid1 = uuid

        Timeline(startServiceContract(uuid1, scope1), startServiceContract(uuid1, scope2))
          .eligiblePrivacyScope() shouldBe
          scope(
            ("CONTACT.EMAIL", "COLLECTION", "ADVERTISING"),
            ("AFFILIATION.MEMBERSHIP", "ANONYMIZATION", "ADVERTISING"),
            ("DEMOGRAPHIC.AGE", "STORING", "MEDICAL"),
            ("DEMOGRAPHIC.AGE", "COLLECTION", "TRACKING")
          )
      }

      it("for start and end service events") {
        val uuid1 = uuid
        val uuid2 = uuid
        val uuid3 = uuid
        val uuid4 = uuid

        val e1  = startServiceContract(uuid1, randomScope(), now - 100)
        val e2  = startServiceContract(uuid2, randomScope(), now - 90)
        val e3  = startServiceNecessary(uuid3, randomScope(), now - 80)
        val e4  = endServiceContract(uuid1, now - 70)
        val e5  = endServiceContract(uuid4, now - 60)
        val e6  = startServiceContract(uuid4, randomScope(), now - 50)
        val e7  = endServiceNecessary(uuid3, now - 40)
        val e8  = startServiceNecessary(uuid, randomScope(), now - 30)
        val e9  = endServiceContract(uuid, now - 20)
        val e10 = endServiceNecessary(uuid, now - 10)

        Timeline(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)
          .eligiblePrivacyScope() shouldBe (e2.getScope union e6.getScope union e8.getScope)
      }

      it("for 1 given consent") {
        val e1 = cgEvent(uuid, randomScope())
        Timeline(e1).eligiblePrivacyScope() shouldBe e1.getScope
      }

      it("for given and revoked consents") {
        val uuid1 = uuid
        val uuid2 = uuid
        val uuid3 = uuid

        val g1  = cgEvent(uuid1, randomScope(), now - 100)
        val g2  = cgEvent(uuid2, randomScope(), now - 90)
        val r1a = crEvent(uuid1, now - 80)
        val g3  = cgEvent(uuid, randomScope(), now - 70)
        val r2  = crEvent(uuid2, now - 60)
        val r1b = crEvent(uuid1, now - 50)
        val r4  = crEvent(uuid3, now - 40)
        val g4  = cgEvent(uuid3, randomScope(), now - 30)

        Timeline(g1, g2, r1a, g3, r2, r1b, r4, g4).eligiblePrivacyScope() shouldBe
          (g3.getScope union g4.getScope)
      }

      // it("for given consents and restrict events") {
      //   val g1 = cgEvent("1", randomScope() union scope1, now - 100)
      //   val g2 = cgEvent("2", randomScope() union scope2, now - 90)
      // }
    }
  }

}
