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

class UserEventsEndpoints(
    userEventsService: UserEventsService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("user-events").tag("User events")

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")

  val giveConsent =
    base
      .description("Add consent for a user")
      .post
      .in("consent")
      .in(jsonBody[GiveConsentPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => userEventsService.addConsentGivenEvent(appId, req))

  val startContract =
    base
      .description("Start service contract for a user")
      .post
      .in("contract")
      .in("start")
      .in(jsonBody[StartContractPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => userEventsService.addStartContractEvent(appId, req))

  val endContract =
    base
      .description("End service contract for a user")
      .post
      .in("contract")
      .in("end")
      .in(jsonBody[EndContractPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => userEventsService.addEndContractEvent(appId, req))

  val startLegitimateInterest =
    base
      .description("Start legitimate interest for a user")
      .post
      .in("legitimate-interest")
      .in("start")
      .in(jsonBody[StartLegitimateInterestPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => userEventsService.addStartLegitimateInterestEvent(appId, req))

  val endLegitimateInterest =
    base
      .description("End legitimate interest for a user")
      .post
      .in("legitimate-interest")
      .in("end")
      .in(jsonBody[EndLegitimateInterestPayload])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .serverLogicSuccess(req => userEventsService.addEndLegitimateInterestEvent(appId, req))

  val endpoints = List(
    giveConsent,
    startContract,
    endContract,
    startLegitimateInterest,
    endLegitimateInterest
  )

}
