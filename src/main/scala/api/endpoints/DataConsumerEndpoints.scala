package io.blindnet.privacy
package api.endpoints

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import services.*
import api.endpoints.messages.privacyrequest.*
import api.endpoints.BaseEndpoint.*
import io.blindnet.privacy.api.endpoints.messages.consumerinterface.PendingDemandPayload

given Configuration = Configuration.default.withSnakeCaseMemberNames

class DataConsumerEndpoints(
    consumerInterfaceService: DataConsumerInterfaceService
) {
  val base = baseEndpoint.in("consumer-interface").tag("Data consumer interface")

  val appId  = "6f083c15-4ada-4671-a6d1-c671bc9105dc"
  val userId = "fdfc95a6-8fd8-4581-91f7-b3d236a6a10e"

  // TODO: add filtering
  val getPendingDemands =
    base
      .description("Get the list of pending privacy request demands")
      .get
      .in("pending-requests")
      .out(jsonBody[List[PendingDemandPayload]])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => consumerInterfaceService.getPendingDemands(appId))

  // val getPendingDemandDetails =
  // val approveDemand =
  // val declineDemand =

  val endpoints = List(getPendingDemands)

}
