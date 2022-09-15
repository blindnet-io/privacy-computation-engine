package io.blindnet.pce
package api.endpoints.messages.configuration

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

case class AddRegulationsPayload(
    regulationIds: List[UUID]
)

object AddRegulationsPayload {
  given Decoder[AddRegulationsPayload] = unSnakeCaseIfy(deriveDecoder[AddRegulationsPayload])
  given Encoder[AddRegulationsPayload] = snakeCaseIfy(deriveEncoder[AddRegulationsPayload])

  given Schema[AddRegulationsPayload] =
    Schema.derived[AddRegulationsPayload](using Configuration.default.withSnakeCaseMemberNames)

}
