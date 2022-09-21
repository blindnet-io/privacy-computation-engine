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

class UserEndpoints(
    userService: UserService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("user").tag("User info")

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")

  val getGivenConsents =
    base
      .description("Get a list of consents user has given")
      .get
      .in("consents")
      // TODO: remove when auth
      .in(header[String]("Authorization"))
      .out(jsonBody[List[GivenConsentsPayload]])
      .serverLogicSuccess(userId => userService.getGivenConsents(appId, userId))

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getGivenConsents)

}
