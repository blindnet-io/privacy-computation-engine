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
import api.endpoints.messages.user.*
import priv.privacyrequest.{ RequestId as PrivReqId }

class UserEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    userService: UserService
) {
  import util.*

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("user").tag("User info")

  val userAuthEndpoint = authenticator.requireUserJwt.secureEndpoint(base)

  val getGivenConsents =
    userAuthEndpoint
      .description("Get a list of consents user has given")
      .get
      .in("consents")
      .out(jsonBody[List[GivenConsentsPayload]])
      .serverLogic(runLogicSuccess(userService.getGivenConsents))

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getGivenConsents)

}
