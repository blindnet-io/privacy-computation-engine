package io.blindnet.privacy
package api.endpoints.messages.privacyrequest

import java.time.Instant

import cats.effect.*
import cats.implicits.*
import io.blindnet.privacy.model.vocabulary.terms.*
import io.blindnet.privacy.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*

case class PrivacyRequestCreatedPayload(
    requestId: String
)

object PrivacyRequestCreatedPayload {
  given Decoder[PrivacyRequestCreatedPayload] =
    unSnakeCaseIfy(deriveDecoder[PrivacyRequestCreatedPayload])

  given Encoder[PrivacyRequestCreatedPayload] =
    snakeCaseIfy(deriveEncoder[PrivacyRequestCreatedPayload])

}
