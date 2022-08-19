package io.blindnet.pce
package api.endpoints.messages.configuration

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

case class PrivacyScopeTriplePayload(
    dc: DataCategory,
    pc: ProcessingCategory,
    pp: Purpose
)

object PrivacyScopeTriplePayload {
  given Decoder[PrivacyScopeTriplePayload] = unSnakeCaseIfy(
    deriveDecoder[PrivacyScopeTriplePayload]
  )

  given Encoder[PrivacyScopeTriplePayload] = snakeCaseIfy(
    deriveEncoder[PrivacyScopeTriplePayload]
  )

  given Schema[PrivacyScopeTriplePayload] =
    Schema.derived[PrivacyScopeTriplePayload](using Configuration.default.withSnakeCaseMemberNames)

}

case class LegalBaseInfoPayload(
    lbType: LegalBaseTerms,
    name: String,
    description: Option[String],
    privacyScope: List[PrivacyScopeTriplePayload]
)

object LegalBaseInfoPayload {
  given Decoder[LegalBaseInfoPayload] = unSnakeCaseIfy(
    deriveDecoder[LegalBaseInfoPayload]
  )

  given Encoder[LegalBaseInfoPayload] = snakeCaseIfy(
    deriveEncoder[LegalBaseInfoPayload]
  )

  given Schema[LegalBaseInfoPayload] =
    Schema.derived[LegalBaseInfoPayload](using Configuration.default.withSnakeCaseMemberNames)

}
