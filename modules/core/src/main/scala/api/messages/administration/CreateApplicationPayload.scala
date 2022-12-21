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

case class CreateApplicationPayload(
    appId: UUID
)

object CreateApplicationPayload {
  given Decoder[CreateApplicationPayload] = unSnakeCaseIfy(deriveDecoder[CreateApplicationPayload])
  given Encoder[CreateApplicationPayload] = snakeCaseIfy(deriveEncoder[CreateApplicationPayload])

  given Schema[CreateApplicationPayload] =
    Schema.derived[CreateApplicationPayload](using Configuration.default.withSnakeCaseMemberNames)

}
