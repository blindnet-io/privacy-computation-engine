package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import api.endpoints.messages.*

case class EndLegitimateInterestPayload(
    dataSubject: DataSubjectPayload,
    legitimateInterestId: UUID,
    date: Instant
)

object EndLegitimateInterestPayload {
  given Decoder[EndLegitimateInterestPayload] = unSnakeCaseIfy(
    deriveDecoder[EndLegitimateInterestPayload]
  )

  given Encoder[EndLegitimateInterestPayload] = snakeCaseIfy(
    deriveEncoder[EndLegitimateInterestPayload]
  )

  given Schema[EndLegitimateInterestPayload] = Schema.derived[EndLegitimateInterestPayload]
}
