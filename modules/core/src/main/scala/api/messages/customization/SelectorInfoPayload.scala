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

case class SelectorInfoPayload(
    name: String,
    dataCategory: DataCategory,
    provenances: List[ProvenanceTerms],
    retentionPolicies: List[RetentionPolicy]
)

object SelectorInfoPayload {
  given Decoder[SelectorInfoPayload] = unSnakeCaseIfy(
    deriveDecoder[SelectorInfoPayload]
  )

  given Encoder[SelectorInfoPayload] = snakeCaseIfy(
    deriveEncoder[SelectorInfoPayload]
  )

  given Schema[SelectorInfoPayload] =
    Schema.derived[SelectorInfoPayload](using Configuration.default.withSnakeCaseMemberNames)

}
