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
import api.endpoints.messages.consumerinterface.*

class DataConsumerEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    consumerInterfaceService: DataConsumerInterfaceService
) extends Endpoints(authenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("consumer-interface").tag("Data consumer interface")

  // TODO: add filtering
  val getPendingDemands =
    appAuthEndpoint
      .description("Get the list of pending privacy request demands")
      .get
      .in("pending-requests")
      .out(jsonBody[List[PendingDemandPayload]])
      .serverLogicSuccess(consumerInterfaceService.getPendingDemands)

  val getPendingDemandDetails =
    appAuthEndpoint
      .description("Get details of a pending privacy request")
      .get
      .in("pending-requests")
      .in(path[UUID]("demandId"))
      .out(jsonBody[PendingDemandDetailsPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.getPendingDemandDetails)

  val approveDemand =
    appAuthEndpoint
      .description("Approve privacy request")
      .post
      .in("pending-requests")
      .in("approve")
      .in(jsonBody[ApproveDemandPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.BadRequest)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.approveDemand)

  val denyDemand =
    appAuthEndpoint
      .description("Deny privacy request")
      .post
      .in("pending-requests")
      .in("deny")
      .in(jsonBody[DenyDemandPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.BadRequest)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.denyDemand)

  val changeRecommendation =
    appAuthEndpoint
      .description("Deny privacy request")
      .post
      .in("pending-requests")
      .in("recommendation")
      .in(jsonBody[ChangeRecommendationPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.BadRequest)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.changeRecommendation)

  val getCompletedDemands =
    appAuthEndpoint
      .description("Get the list of completed privacy request demands")
      .get
      .in("completed-requests")
      .out(jsonBody[List[CompletedDemandPayload]])
      .serverLogicSuccess(consumerInterfaceService.getCompletedDemands)

  val getCompletedDemandDetails =
    appAuthEndpoint
      .description("Get details of a completed demand")
      .get
      .in("completed-requests")
      .in(path[UUID]("requestId"))
      .out(jsonBody[List[CompletedDemandInfoPayload]])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.getCompletedDemandInfo)

  val getTimeline =
    appAuthEndpoint
      .description("Get user's timeline")
      .get
      .in("timeline")
      .in(path[String]("userId"))
      .out(jsonBody[TimelineEventsPayload])
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.UnprocessableEntity)))
      .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound)))
      .serverLogicSuccess(consumerInterfaceService.getTimeline)

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
