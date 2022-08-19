package io.blindnet.pce
package api.endpoints.messages.configuration

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*

case class CreateRetentionPolicyPayload(
    dataCategory: DataCategory,
    policy: RetentionPolicyTerms,
    duration: String,
    after: EventTerms
)

object CreateRetentionPolicyPayload {
  given Decoder[CreateRetentionPolicyPayload] = deriveDecoder[CreateRetentionPolicyPayload]
  given Encoder[CreateRetentionPolicyPayload] = deriveEncoder[CreateRetentionPolicyPayload]

  given Schema[CreateRetentionPolicyPayload] = Schema.derived[CreateRetentionPolicyPayload]

}