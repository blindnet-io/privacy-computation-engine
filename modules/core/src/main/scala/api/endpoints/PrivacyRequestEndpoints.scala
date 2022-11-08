package io.blindnet.pce
package api.endpoints

import api.endpoints.Endpoints
import api.endpoints.messages.privacyrequest.*
import priv.privacyrequest.{ RequestId as PrivReqId }
import services.*
import cats.effect.IO
import io.blindnet.identityclient.auth.*
import io.circe.generic.auto.*
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

import java.util.UUID

class PrivacyRequestEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    reqService: PrivacyRequestService
) extends Endpoints(authenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("privacy-request").tag("Privacy requests")

  val createPrivacyRequest =
    anyUserAuthEndpoint
      .description("Create a privacy request")
      .post
      .in(jsonBody[CreatePrivacyRequestPayload])
      .out(jsonBody[PrivacyRequestCreatedPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .serverLogicSuccess(reqService.createPrivacyRequest)

  val getRequestHistory =
    userAuthEndpoint
      .description("Get history of privacy requests")
      .get
      .in("history")
      .out(jsonBody[RequestHistoryPayload])
      .serverLogicSuccess(reqService.getRequestHistory)

  val getReqStatus =
    anyUserAuthEndpoint
      .description("Get privacy request status")
      .get
      .in(path[PrivReqId]("requestId"))
      .out(jsonBody[List[PrivacyResponsePayload]])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(reqService.getResponse)

  val cancelDemand =
    userAuthEndpoint
      .description("Cancel a pending demand")
      .post
      .in("cancel")
      .in(jsonBody[CancelDemandPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .serverLogicSuccess(reqService.cancelDemand)

  val endpoints = List(createPrivacyRequest, getRequestHistory, getReqStatus, cancelDemand)

}
