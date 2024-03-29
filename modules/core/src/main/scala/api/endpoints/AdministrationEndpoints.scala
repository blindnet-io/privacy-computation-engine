package io.blindnet.pce
package api.endpoints

import cats.effect.IO
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import services.*
import api.endpoints.messages.administration.*
import io.blindnet.identityclient.auth.*

class AdministrationEndpoints(
    authenticator: ConstAuthenticator[Unit],
    administrationService: AdministrationService
) {
  import util.*

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  lazy val Tag = "Administration"

  val base = baseEndpoint.in("admin").tag(Tag)

  val authEndpoint = authenticator.secureEndpoint(base)

  val createApp =
    authEndpoint
      .description("Create a new application")
      .put
      .in("applications")
      .in(jsonBody[CreateApplicationPayload])
      .errorOutVariant(unprocessable)
      .serverLogic(runLogic(administrationService.createApp))

  val endpoints = List(
    createApp
  )

}
