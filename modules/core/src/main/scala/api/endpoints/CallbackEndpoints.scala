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
import api.endpoints.messages.callback.*

class CallbackEndpoints(
    callbacksService: CallbackService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("callback").tag("Callbacks")

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")

  val cb =
    base
      .description("Link to access data in the storage created")
      .post
      .in(path[UUID])
      .in(jsonBody[DataCallbackPayload])
      .serverLogicSuccess((id, req) => callbacksService.handle(appId, id, req))

  val endpoints = List(cb)

}
