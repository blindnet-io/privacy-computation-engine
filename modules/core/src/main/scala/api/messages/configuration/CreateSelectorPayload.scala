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

case class CreateSelectorPayload(
    @description("selector name")
    @encodedExample("HOME")
    name: String,
    @description("selector is the subcategory of this data category")
    @encodedExample("CONTACT.ADDRESS")
    dataCategory: DataCategory
)

object CreateSelectorPayload {
  given Decoder[CreateSelectorPayload] = unSnakeCaseIfy(deriveDecoder[CreateSelectorPayload])
  given Encoder[CreateSelectorPayload] = snakeCaseIfy(deriveEncoder[CreateSelectorPayload])

  given Schema[CreateSelectorPayload] =
    Schema.derived[CreateSelectorPayload](using Configuration.default.withSnakeCaseMemberNames)

}
