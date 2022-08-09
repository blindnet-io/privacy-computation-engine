package io.blindnet.pce
package api.endpoints

import sttp.tapir.*

object BaseEndpoint {

  val baseEndpoint =
    endpoint.in("v0")

}
