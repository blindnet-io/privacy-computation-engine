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
import api.endpoints.messages.callback.*

class CallbackEndpoints(
    callbacksService: CallbackHandler
) {
  import util.*

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("callback").tag("Callbacks")

  val cb =
    base
      .description("Handle callback")
      .post
      .in(path[UUID]("callbackId"))
      .in(jsonBody[DataCallbackPayload])
      .serverLogicSuccess((id, req) => callbacksService.handle(id, req))

  val endpoints = List(cb)

}
