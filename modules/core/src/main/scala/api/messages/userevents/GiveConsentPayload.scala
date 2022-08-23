package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.util.UUID

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import java.time.Instant
import sttp.tapir.generic.Configuration

case class GiveConsentPayload(
    dataSubject: DataSubject,
    consentId: UUID,
    date: Instant
)

object GiveConsentPayload {
  given Decoder[GiveConsentPayload] = unSnakeCaseIfy(deriveDecoder[GiveConsentPayload])
  given Encoder[GiveConsentPayload] = snakeCaseIfy(deriveEncoder[GiveConsentPayload])

  given Schema[GiveConsentPayload] = Schema.derived[GiveConsentPayload]
}
