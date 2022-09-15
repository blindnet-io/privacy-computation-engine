package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import weaver.*

object ProcessingCategorySuite extends FunSuite {

  test("granularize All") {
    expect(
      ProcessingCategory.granularize(ProcessingCategory.All) == Set(
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
    )
  }

  test("granularize ANONYMIZATION") {
    expect(
      ProcessingCategory.granularize(ProcessingCategory("ANONYMIZATION")) == Set(
        "ANONYMIZATION"
      ).map(ProcessingCategory(_))
    )
  }

}
