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

class ProcessingCategorySpec extends UnitSpec {

  describe("ProcessingCategory") {
    describe("should resolve to most granular categories") {
      it("for All category") {
        ProcessingCategory.granularize(ProcessingCategory.All) shouldBe Set(
          "ANONYMIZATION",
          "AUTOMATED-INFERENCE",
          "AUTOMATED-DECISION-MAKING",
          "COLLECTION",
          "GENERATING",
          "PUBLISHING",
          "STORING",
          "SHARING",
          "USING",
          "OTHER-PROCESSING"
        ).map(ProcessingCategory(_))
      }

      it("for lowest level category") {
        ProcessingCategory.granularize(ProcessingCategory("ANONYMIZATION")) shouldBe Set(
          "ANONYMIZATION"
        ).map(ProcessingCategory(_))
      }

    }
  }

}
