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

class ActionSpec extends UnitSpec {

  describe("Action") {
    describe("withSubCategories") {
      it("should return itself if it doesn't contain subcategories") {
        Action.Access.withSubCategories() shouldBe List(Action.Access)
      }

      it("should return list of subcategories") {
        Action.Transparency.withSubCategories().toSet shouldBe
          Set(
            Action.Transparency,
            Action.TDataCategories,
            Action.TDPO,
            Action.TKnown,
            Action.TLegalBases,
            Action.TOrganization,
            Action.TPolicy,
            Action.TProcessingCategories,
            Action.TProvenance,
            Action.TPurpose,
            Action.TRetention,
            Action.TWhere,
            Action.TWho
          )
      }
    }
    describe("granularize") {
      it("should return empty list") {
        Action.Access.granularize() shouldBe List(Action.Access)
      }

      it("should return list of subcategories") {
        Action.Transparency.granularize().toSet shouldBe
          Set(
            Action.TDataCategories,
            Action.TDPO,
            Action.TKnown,
            Action.TLegalBases,
            Action.TOrganization,
            Action.TPolicy,
            Action.TProcessingCategories,
            Action.TProvenance,
            Action.TPurpose,
            Action.TRetention,
            Action.TWhere,
            Action.TWho
          )
      }
    }
  }

}
