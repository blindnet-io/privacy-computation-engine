package io.blindnet.privacy
package api.endpoints.messages.privacyrequest

import cats.effect.*
import io.blindnet.privacy.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.Configuration

case class BadPrivacyRequestPayload(
    error: String
)

object BadPrivacyRequestPayload {
  given Decoder[BadPrivacyRequestPayload] = deriveDecoder[BadPrivacyRequestPayload]
  given Encoder[BadPrivacyRequestPayload] = deriveEncoder[BadPrivacyRequestPayload]

  given Schema[BadPrivacyRequestPayload] =
    Schema.derived[BadPrivacyRequestPayload]

}
