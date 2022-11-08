package io.blindnet.pce
package api.endpoints

import java.util.UUID

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
import services.*
import api.endpoints.messages.privacyrequest.*
import api.endpoints.BaseEndpoint.*
import io.blindnet.pce.model.error.*
import cats.implicits.*

class UserEventsEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    userEventsService: UserEventsService
) extends Endpoints(authenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("user-events").tag("User events")

  val giveConsentUnsafe =
    publicEndpoint
      .description("Give consent")
      .post
      .in("consent")
      .in("unsafe")
      .in(jsonBody[GiveConsentUnsafePayload])
      .errorOutVariants(notFound)
      .serverLogic(req => userEventsService.addConsentGivenEvent(req).attempt)

  val giveConsent =
    userAuthEndpoint
      .description("Give consent")
      .post
      .in("consent")
      .in(jsonBody[GiveConsentPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.addConsentGivenEvent))

  val storeGivenConsent =
    appAuthEndpoint
      .description("Store given consent for a user")
      .post
      .in("consent")
      .in("store")
      .in(jsonBody[StoreGivenConsentPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.storeGivenConsentEvent))

  val startContract =
    appAuthEndpoint
      .description("Start service contract for a user")
      .post
      .in("contract")
      .in("start")
      .in(jsonBody[StartContractPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.addStartContractEvent))

  val endContract =
    appAuthEndpoint
      .description("End service contract for a user")
      .post
      .in("contract")
      .in("end")
      .in(jsonBody[EndContractPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.addEndContractEvent))

  val startLegitimateInterest =
    appAuthEndpoint
      .description("Start legitimate interest for a user")
      .post
      .in("legitimate-interest")
      .in("start")
      .in(jsonBody[StartLegitimateInterestPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.addStartLegitimateInterestEvent))

  val endLegitimateInterest =
    appAuthEndpoint
      .description("End legitimate interest for a user")
      .post
      .in("legitimate-interest")
      .in("end")
      .in(jsonBody[EndLegitimateInterestPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(userEventsService.addEndLegitimateInterestEvent))

  val endpoints = List(
    giveConsentUnsafe,
    giveConsent,
    storeGivenConsent,
    startContract,
    endContract,
    startLegitimateInterest,
    endLegitimateInterest
  )

}
