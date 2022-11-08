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
import priv.privacyrequest.{ RequestId as PrivReqId }

class UserEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    userService: UserService
) extends Endpoints(authenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("user").tag("User info")

  val getGivenConsents =
    userAuthEndpoint
      .description("Get a list of consents user has given")
      .get
      .in("consents")
      .out(jsonBody[List[GivenConsentsPayload]])
      .serverLogic(runLogicOnlyAuth(userService.getGivenConsents))

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getGivenConsents)

}
