package io.blindnet.privacy
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

class PrivacyRequestEndpoints(
    reqService: PrivacyRequestService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("privacy-request").tag("Privacy requests")

  val appId  = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")
  val userId = "fdfc95a6-8fd8-4581-91f7-b3d236a6a10e"

  val createPrivacyRequest =
    base
      .description("Create a privacy request")
      .post
      .in("create")
      .in(jsonBody[CreatePrivacyRequestPayload])
      .out(jsonBody[PrivacyRequestCreatedPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(jsonBody[BadPrivacyRequestPayload])
      .serverLogicSuccess(req => reqService.createPrivacyRequest(req, appId))

  val getRequestHistory =
    base
      .description("Get history of privacy requests")
      .get
      .in("history")
      .out(jsonBody[RequestHistoryPayload])
      .serverLogicSuccess(_ => reqService.getRequestHistory(appId, userId))

  val getRequestStatus =
    base

  val getReqStatus =
    base
      .description("Get privacy request status")
      .get
      .in("status")
      .in(path[UUID]("requestId"))
      .out(jsonBody[List[PrivacyResponsePayload]])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(reqId => reqService.getResponse(reqId, appId, userId))

  val endpoints = List(createPrivacyRequest, getRequestHistory, getReqStatus)

}
