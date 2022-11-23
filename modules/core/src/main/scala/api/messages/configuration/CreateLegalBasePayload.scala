package io.blindnet.pce
package api.endpoints.messages.configuration

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.api.endpoints.messages.ScopePayload

case class CreateLegalBasePayload(
    @description("type of the legal base")
    @encodedExample("CONSENT")
    lbType: LegalBaseTerms,
    @description("legal base name")
    @encodedExample("Contact form")
    name: Option[String],
    @description("legal base description")
    @encodedExample("Collection of the contact data for advertising")
    description: Option[String],
    @description("privacy scope of the legal base")
    // format: off
    @encodedExample(Set(ScopePayload(Set(DataCategory("*")), Set(ProcessingCategory("USING"), ProcessingCategory("SHARING")), Set(Purpose("ADVERTISING")))).asJson)
    // format: on
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
