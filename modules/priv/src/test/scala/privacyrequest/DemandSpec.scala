package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.privacyrequest.Demand
import cats.implicits.*
import weaver.*

object DemandSuite extends FunSuite {

  object fixtures {}

  import fixtures.*
  import io.blindnet.pce.priv.test.*

  test("demand should be validated") {
    val d = demand(Action.Access)
    expect(Demand.validate(d) == d.valid)
  }

  test("restrictions should be retrievable from demand") {
    val (psR, consentR, dateR, provR, dataRefR) = (ps(), consent(), date(), prov(), dataRef())
    val d = demand(Action.Access, List(psR, consentR, dateR, provR, dataRefR))

    expect(d.getPSR.get == psR.scope) and
      expect(d.getConsentR.get == consentR.consentId) and
      expect(d.getDateRangeR.get == (dateR.from, dateR.to)) and
      expect(d.getProvenanceR.get == (provR.term, provR.target)) and
      expect(d.getDataRefR.get == dataRefR.dataReferences)
  }

}
