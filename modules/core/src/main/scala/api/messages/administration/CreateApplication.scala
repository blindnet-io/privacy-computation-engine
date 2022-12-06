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

case class CreateApplication(
    appId: UUID
)

object CreateApplication {
  given Decoder[CreateApplication] = unSnakeCaseIfy(deriveDecoder[CreateApplication])
  given Encoder[CreateApplication] = snakeCaseIfy(deriveEncoder[CreateApplication])

  given Schema[CreateApplication] =
    Schema.derived[CreateApplication](using Configuration.default.withSnakeCaseMemberNames)

}
