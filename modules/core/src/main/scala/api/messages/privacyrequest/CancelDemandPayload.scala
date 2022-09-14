package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.util.UUID

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration

case class CancelDemandPayload(
    demandId: UUID
)

object CancelDemandPayload {
  given Decoder[CancelDemandPayload] = unSnakeCaseIfy(deriveDecoder[CancelDemandPayload])

  given Encoder[CancelDemandPayload] = snakeCaseIfy(deriveEncoder[CancelDemandPayload])

  given Schema[CancelDemandPayload] =
    Schema.derived[CancelDemandPayload](using Configuration.default.withSnakeCaseMemberNames)

}
