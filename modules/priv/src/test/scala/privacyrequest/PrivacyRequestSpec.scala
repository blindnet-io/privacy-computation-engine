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
import weaver.*

object PrivacyRequestSuite extends FunSuite {

  object fixtures {}

  import fixtures.*
  import io.blindnet.pce.priv.test.*

  test("privacy request should be validated") {
    val demands = List(
      demand(Action.Access),
      demand(Action.Delete),
      demand(Action.Object)
    )
    val pr      = request(demands)

    val res = PrivacyRequest.validateDemands(pr)
    expect((res._1, res._2.toSet) == (List.empty, demands.toSet))
  }

}
