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

case class PrivacyScopeDimensionsPayload(
    dataCategories: Set[DataCategory],
    processingCategories: Set[ProcessingCategory],
    purposes: Set[Purpose]
)

object PrivacyScopeDimensionsPayload {
  given Decoder[PrivacyScopeDimensionsPayload] = unSnakeCaseIfy(
    deriveDecoder[PrivacyScopeDimensionsPayload]
  )

  given Encoder[PrivacyScopeDimensionsPayload] = snakeCaseIfy(
    deriveEncoder[PrivacyScopeDimensionsPayload]
  )

  given Schema[PrivacyScopeDimensionsPayload] =
    Schema.derived[PrivacyScopeDimensionsPayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

}
