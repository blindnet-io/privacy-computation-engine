package io.blindnet.pce
package api.endpoints.messages.callback

import java.time.Instant
import java.util.UUID

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class CallbackMsgPayload(
    msg: Option[String]
)

object CallbackMsgPayload {
  given Decoder[CallbackMsgPayload] = unSnakeCaseIfy(deriveDecoder[CallbackMsgPayload])
  given Encoder[CallbackMsgPayload] = snakeCaseIfy(deriveEncoder[CallbackMsgPayload])

  given Schema[CallbackMsgPayload] =
    Schema.derived[CallbackMsgPayload](using Configuration.default.withSnakeCaseMemberNames)

}
