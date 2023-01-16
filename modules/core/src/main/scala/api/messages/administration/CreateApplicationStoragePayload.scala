package io.blindnet.pce
package api.endpoints.messages.administration

import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class CreateApplicationStoragePayload(
    url: String,
    token: String
)

object CreateApplicationStoragePayload {
  given Decoder[CreateApplicationStoragePayload] = unSnakeCaseIfy(
    deriveDecoder[CreateApplicationStoragePayload]
  )

  given Encoder[CreateApplicationStoragePayload] = snakeCaseIfy(
    deriveEncoder[CreateApplicationStoragePayload]
  )

  given Schema[CreateApplicationStoragePayload] =
    Schema.derived[CreateApplicationStoragePayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

}
