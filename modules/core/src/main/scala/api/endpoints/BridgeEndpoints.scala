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
import api.endpoints.messages.bridge.*

class BridgeEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    bridgeService: BridgeService
) {
  import util.*

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("bridge").tag("Bridge")

  val appAuthEndpoint = authenticator.requireAppJwt.secureEndpoint(base)

  // TODO: add filtering
  val getPendingDemands =
    appAuthEndpoint
      .description("Get the list of pending privacy request demands")
      .get
      .in("pending-requests")
      .out(jsonBody[List[PendingDemandPayload]])
      .serverLogic(runLogicSuccess(bridgeService.getPendingDemands))

  val getPendingDemandDetails =
    appAuthEndpoint
      .description("Get details of a pending privacy request")
      .get
      .in("pending-requests")
      .in(path[UUID]("demandId"))
      .out(jsonBody[PendingDemandDetailsPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(bridgeService.getPendingDemandDetails))

  val approveDemand =
    appAuthEndpoint
      .description("Approve privacy request")
      .post
      .in("pending-requests")
      .in("approve")
      .in(jsonBody[ApproveDemandPayload])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(bridgeService.approveDemand))

  val denyDemand =
    appAuthEndpoint
      .description("Deny privacy request")
      .post
      .in("pending-requests")
      .in("deny")
      .in(jsonBody[DenyDemandPayload])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(bridgeService.denyDemand))

  val changeRecommendation =
    appAuthEndpoint
      .description("Deny privacy request")
      .post
      .in("pending-requests")
      .in("recommendation")
      .in(jsonBody[ChangeRecommendationPayload])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(bridgeService.changeRecommendation))

  val getCompletedDemands =
    appAuthEndpoint
      .description("Get the list of completed privacy request demands")
      .get
      .in("completed-requests")
      .out(jsonBody[List[CompletedDemandPayload]])
      .serverLogic(runLogicSuccess(bridgeService.getCompletedDemands))

  val getCompletedDemandDetails =
    appAuthEndpoint
      .description("Get details of a completed demand")
      .get
      .in("completed-requests")
      .in(path[UUID]("requestId"))
      .out(jsonBody[List[CompletedDemandInfoPayload]])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(bridgeService.getCompletedDemandInfo))

  val getTimeline =
    appAuthEndpoint
      .description("Get user's timeline")
      .get
      .in("timeline")
      .in(path[String]("userId"))
      .out(jsonBody[TimelineEventsPayload])
      .serverLogic(runLogicSuccess(bridgeService.getTimeline))

  val endpoints =
    List(
      getPendingDemands,
      getPendingDemandDetails,
      approveDemand,
      denyDemand,
      changeRecommendation,
      getCompletedDemands,
      getCompletedDemandDetails,
      getTimeline
    )

}
