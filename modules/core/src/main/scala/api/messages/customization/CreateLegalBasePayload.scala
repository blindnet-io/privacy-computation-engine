package io.blindnet.pce
package api.endpoints.messages.customization

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

case class ScopePayload(
    dc: DataCategory,
    pc: ProcessingCategory,
    pp: Purpose
)

object ScopePayload {
  given Decoder[ScopePayload] = deriveDecoder[ScopePayload]

  given Encoder[ScopePayload] = deriveEncoder[ScopePayload]

  given Schema[ScopePayload] = Schema.derived[ScopePayload]
}

case class CreateLegalBasePayload(
    lbType: LegalBaseTerms,
    name: Option[String],
    description: Option[String],
    scope: Set[ScopePayload]
)

object CreateLegalBasePayload {
  given Decoder[CreateLegalBasePayload] = unSnakeCaseIfy(
    deriveDecoder[CreateLegalBasePayload]
  )

  given Encoder[CreateLegalBasePayload] = snakeCaseIfy(
    deriveEncoder[CreateLegalBasePayload]
  )

  given Schema[CreateLegalBasePayload] =
    Schema.derived[CreateLegalBasePayload](using Configuration.default.withSnakeCaseMemberNames)

}
