package io.blindnet.pce
package api.endpoints.messages.configuration

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

case class RegulationResponsePayload(
    id: UUID,
    name: String,
    description: Option[String]
)

object RegulationResponsePayload {

  def fromRegulationInfo(r: RegulationInfo) =
    RegulationResponsePayload(r.id, r.name, r.description)

  given Decoder[RegulationResponsePayload] = unSnakeCaseIfy(
    deriveDecoder[RegulationResponsePayload]
  )

  given Encoder[RegulationResponsePayload] = snakeCaseIfy(
    deriveEncoder[RegulationResponsePayload]
  )

  given Schema[RegulationResponsePayload] =
    Schema.derived[RegulationResponsePayload](using Configuration.default.withSnakeCaseMemberNames)

}
