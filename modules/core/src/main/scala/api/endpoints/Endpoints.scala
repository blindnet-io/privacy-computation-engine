package io.blindnet.pce
package api.endpoints

import io.blindnet.identityclient.auth.*
import sttp.tapir.{ PublicEndpoint, * }

type EndpointT = PublicEndpoint[Unit, Unit, Unit, Any]

abstract class Endpoints(authenticator: JwtAuthenticator[Jwt]) extends EndpointsUtil {
  def mapEndpoint(endpoint: EndpointT): EndpointT = endpoint

  val baseEndpoint        = endpoint.in("v0")
  val mappedAuthenticator = authenticator.withBaseEndpoint(mapEndpoint(baseEndpoint))

  val publicEndpoint        = mapEndpoint(baseEndpoint)
  val anyAuthEndpoint       = mappedAuthenticator.secureEndpoint
  val appAuthEndpoint       = mappedAuthenticator.requireAppJwt.secureEndpoint
  val anyUserAuthEndpoint   = mappedAuthenticator.requireAnyUserJwt.secureEndpoint
  val userAuthEndpoint      = mappedAuthenticator.requireUserJwt.secureEndpoint
  val anonymousAuthEndpoint = mappedAuthenticator.requireAnonymousJwt.secureEndpoint
}
