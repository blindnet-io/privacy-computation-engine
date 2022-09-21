package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.util.UUID

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import java.time.Instant

case class GivenConsentsPayload(
    id: UUID,
    name: Option[String],
    date: Instant
)

object GivenConsentsPayload {
  given Decoder[GivenConsentsPayload] = unSnakeCaseIfy(deriveDecoder[GivenConsentsPayload])
  given Encoder[GivenConsentsPayload] = snakeCaseIfy(deriveEncoder[GivenConsentsPayload])

  given Schema[GivenConsentsPayload] =
    Schema.derived[GivenConsentsPayload](using Configuration.default.withSnakeCaseMemberNames)

}
