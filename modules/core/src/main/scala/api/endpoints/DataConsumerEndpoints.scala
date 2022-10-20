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
    base
      .description("Deny privacy request")
      .post
      .in("pending-requests")
      .in("recommendation")
      .in(jsonBody[ChangeRecommendationPayload])
      .errorOut(statusCode(StatusCode.BadRequest))
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(req => consumerInterfaceService.changeRecommendation(appId, req))

  val getCompletedDemands =
    base
      .description("Get the list of completed privacy request demands")
      .get
      .in("completed-requests")
      .out(jsonBody[List[CompletedDemandPayload]])
      .serverLogicSuccess(req => consumerInterfaceService.getCompletedDemands(appId))

  val getCompletedDemandDetails =
    base
      .description("Get details of a completed demand")
      .get
      .in("completed-requests")
      .in(path[UUID]("requestId"))
      .out(jsonBody[List[CompletedDemandInfoPayload]])
      .errorOut(statusCode(StatusCode.UnprocessableEntity))
      .errorOut(statusCode(StatusCode.NotFound))
      .serverLogicSuccess(dId => consumerInterfaceService.getCompletedDemandInfo(appId, dId))

  val endpoints =
    List(
      getPendingDemands,
      getPendingDemandDetails,
      approveDemand,
      denyDemand,
      changeRecommendation,
      getCompletedDemands,
      getCompletedDemandDetails
    )

}
