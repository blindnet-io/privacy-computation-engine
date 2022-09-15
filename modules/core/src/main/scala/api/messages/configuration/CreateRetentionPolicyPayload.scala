package io.blindnet.pce
package api.endpoints.messages.configuration

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

@description("keep CONTACT for no longer than 30 days after a service defined by a legal base ends")
case class CreateRetentionPolicyPayload(
    @description("data category for which the policy is created")
    @encodedExample("CONTACT")
    dataCategory: DataCategory,
    @description("retention policy")
    @encodedExample("NO-LONGER-THAN")
    policy: RetentionPolicyTerms,
    @description("duration in JSON Schema duration format")
    @encodedExample("P30D")
    duration: String,
    @description("event type to which the retention duration is relative to")
    @encodedExample("SERVICE-END")
    after: EventTerms
)

object CreateRetentionPolicyPayload {
  given Decoder[CreateRetentionPolicyPayload] =
    unSnakeCaseIfy(deriveDecoder[CreateRetentionPolicyPayload])

  given Encoder[CreateRetentionPolicyPayload] =
    snakeCaseIfy(deriveEncoder[CreateRetentionPolicyPayload])

  given Schema[CreateRetentionPolicyPayload] = Schema
    .derived[CreateRetentionPolicyPayload](using Configuration.default.withSnakeCaseMemberNames)

}
