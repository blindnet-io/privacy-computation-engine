package io.blindnet.pce
package priv

import java.time.Instant
import io.blindnet.pce.priv.terms.*
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.util.*
import io.blindnet.pce.priv.PS
import weaver.*

object ActionSuite extends FunSuite {

  test("withSubCategories returns only provided Action with no subcategories") {
    expect(Action.Access.withSubCategories() == List(Action.Access))
  }

  test("withSubCategories returns provided Action with its' subcategories") {
    expect(
      Action.Transparency.withSubCategories().toSet ==
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
    )
  }

  test("granularize returns empty list for action with no subcategories") {
    expect(Action.Access.granularize() == List(Action.Access))
  }

  test("granularize returns list of subcategories for the provided action") {
    expect(
      Action.Transparency.granularize().toSet ==
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
    )
  }

}
