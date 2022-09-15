package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.time.Instant
import java.util.UUID

import cats.effect.*
import cats.implicits.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import priv.terms.*

case class PrivacyRequestCreatedPayload(
    requestId: UUID
)

object PrivacyRequestCreatedPayload {
  given Decoder[PrivacyRequestCreatedPayload] =
    unSnakeCaseIfy(deriveDecoder[PrivacyRequestCreatedPayload])

  given Encoder[PrivacyRequestCreatedPayload] =
    snakeCaseIfy(deriveEncoder[PrivacyRequestCreatedPayload])

}
