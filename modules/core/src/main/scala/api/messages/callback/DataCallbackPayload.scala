package io.blindnet.pce
package api.endpoints.messages.callback

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*

case class DataCallbackPayload(
    request_id: String,
    accepted: Boolean,
    data_url: Option[String]
)

object DataCallbackPayload {
  given Decoder[DataCallbackPayload] = deriveDecoder[DataCallbackPayload]
  given Encoder[DataCallbackPayload] = deriveEncoder[DataCallbackPayload]

  given Schema[DataCallbackPayload] =
    Schema.derived[DataCallbackPayload]

}
