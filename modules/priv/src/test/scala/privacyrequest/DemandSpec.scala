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
import io.blindnet.pce.priv.privacyrequest.Demand
import cats.implicits.*

class DemandSpec extends UnitSpec {

  object fixtures {}

  import fixtures.*
  import test.*

  describe("Demand") {
    it("should be validated") {
      val d = demand(Action.Access)
      Demand.validate(d) shouldBe d.valid
    }

    describe("restrictions") {
      it("should be easily retrievable") {
        val (psR, consentR, dateR, provR, dataRefR) = (ps(), consent(), date(), prov(), dataRef())
        val d = demand(Action.Access, List(psR, consentR, dateR, provR, dataRefR))

        d.getPSR.get shouldBe psR.scope
        d.getConsentR.get shouldBe consentR.consentId
        d.getDateRangeR.get shouldBe (dateR.from, dateR.to)
        d.getProvenanceR.get shouldBe (provR.term, provR.target)
        d.getDataRefR.get shouldBe dataRefR.dataReferences
      }
    }
  }

}
