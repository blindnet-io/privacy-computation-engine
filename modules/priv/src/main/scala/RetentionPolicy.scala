package io.blindnet.pce
package priv

import terms.*
import io.blindnet.pce.priv.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class RetentionPolicy(
    policyType: RetentionPolicyTerms,
    // TODO: https://json-schema.org/draft/2020-12/json-schema-validation.html#name-dates-times-and-duration
    duration: String,
    after: EventTerms
)

object RetentionPolicy {
  given Decoder[RetentionPolicy] = unSnakeCaseIfy(deriveDecoder[RetentionPolicy])

  given Encoder[RetentionPolicy] = snakeCaseIfy(deriveEncoder[RetentionPolicy])

  given Schema[RetentionPolicy] =
    Schema.derived[RetentionPolicy](using Configuration.default.withSnakeCaseMemberNames)

}
