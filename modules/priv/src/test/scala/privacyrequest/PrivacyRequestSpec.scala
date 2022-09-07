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
import io.blindnet.pce.priv.privacyrequest.*
import cats.implicits.*

class PrivacyRequestSpec extends UnitSpec {

  object fixtures {}

  import fixtures.*
  import test.*

  describe("Privacy request") {
    it("should be validated") {
      val demands = List(
        demand(Action.Access),
        demand(Action.Delete),
        demand(Action.Object)
      )
      val pr      = request(demands)

      val res = PrivacyRequest.validateDemands(pr)
      (res._1, res._2.toSet) shouldBe (List.empty, demands.toSet)
    }
  }

}
