package io.blindnet.pce
package api.endpoints

import java.util.UUID

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
import api.endpoints.messages.consumerinterface.*

class DataConsumerEndpoints(
    consumerInterfaceService: DataConsumerInterfaceService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("consumer-interface").tag("Data consumer interface")

  val appId  = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")
  val userId = "fdfc95a6-8fd8-4581-91f7-b3d236a6a10e"

  // TODO: add filtering
  val getPendingDemands =
    base
      .description("Get the list of pending privacy request demands")
      .get
      .in("pending-requests")
      .out(jsonBody[List[PendingDemandPayload]])
      .serverLogicSuccess(req => consumerInterfaceService.getPendingDemands(appId))

  val getPendingDemandDetails =
    base
      .description("Get details of a pending privacy request")
      .get
      .in("pending-requests")
      .in(path[UUID]("demandId"))
      .out(jsonBody[PendingDemandDetailsPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(dId => consumerInterfaceService.getPendingDemandDetails(appId, dId))

  val approveDemand =
    base
      .description("Approve privacy request")
      .post
      .in("pending-requests")
      .in("approve")
      .in(jsonBody[ApproveDemandPayload])
      .errorOut(statusCode(StatusCode.BadRequest))
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(req => consumerInterfaceService.approveDemand(appId, req))

  // val declineDemand =

  val endpoints = List(getPendingDemands, getPendingDemandDetails, approveDemand)

}
