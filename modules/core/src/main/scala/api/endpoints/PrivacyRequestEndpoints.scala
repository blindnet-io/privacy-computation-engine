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
import priv.privacyrequest.{ RequestId as PrivReqId }

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
      .in(jsonBody[CreatePrivacyRequestPayload])
      .out(jsonBody[PrivacyRequestCreatedPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => reqService.createPrivacyRequest(req, appId))

  val getRequestHistory =
    base
      .description("Get history of privacy requests")
      .get
      .in("history")
      // TODO: remove when auth
      .in(header[String]("Authorization"))
      .out(jsonBody[RequestHistoryPayload])
      .serverLogicSuccess(uId => reqService.getRequestHistory(appId, uId))

  val getReqStatus =
    base
      .description("Get privacy request status")
      .get
      .in(path[UUID]("requestId"))
      // TODO: remove when auth
      .in(header[String]("Authorization"))
      .out(jsonBody[List[PrivacyResponsePayload]])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(
        (reqId, uId) => reqService.getResponse(PrivReqId(reqId), appId, Some(uId))
      )

  val cancelDemand =
    base
      .description("Cancel a pending demand")
      .post
      .in("cancel")
      // TODO: remove when auth
      .in(header[String]("Authorization"))
      .in(jsonBody[CancelDemandPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess((uId, req) => reqService.cancelDemand(req, appId, uId))

  val endpoints = List(createPrivacyRequest, getRequestHistory, getReqStatus, cancelDemand)

}
