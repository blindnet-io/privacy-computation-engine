package io.blindnet.privacy
package api.endpoints

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.server.Router
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import services.*
import api.endpoints.payload.request.*
import api.endpoints.payload.response.*
import api.endpoints.BaseEndpoint.*

given Configuration = Configuration.default.withSnakeCaseMemberNames

class PrivacyRequestEndpoints(
    reqService: PrivacyRequestService
) {
  val base = baseEndpoint.tag("Privacy requests")

  val appId = "6f083c15-4ada-4671-a6d1-c671bc9105dc"

  val privacyRequest =
    base
      .description("privacy request")
      .post
      .in("privacy-request")
      .in(jsonBody[PrivacyRequestPayload])
      .out(jsonBody[PrivacyRequestResponsePayload])
      .serverLogicSuccess(req => reqService.processRequest(req, appId))

  val endpoints = List(privacyRequest)

}
