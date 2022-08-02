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

given Configuration = Configuration.default.withSnakeCaseMemberNames

class PrivacyRequestEndpoints(
    reqService: PrivacyRequestService
) {
  val base = baseEndpoint.in("privacy-request").tag("Privacy requests")

  val appId = "6f083c15-4ada-4671-a6d1-c671bc9105dc"

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

  val getReqStatus =
    base
      .description("Get privacy request status")
      .get
      .in("status")
      .in(path[String]("requestId"))
      .out(jsonBody[List[PrivacyResponsePayload]])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(reqId => reqService.getResponse(reqId, appId))

  val endpoints = List(createPrivacyRequest, getReqStatus)

}
