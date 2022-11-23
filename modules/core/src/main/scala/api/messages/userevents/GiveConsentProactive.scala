package io.blindnet.pce
package api.endpoints.messages.userevents

import java.time.Instant
import java.util.UUID

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
import io.blindnet.pce.api.endpoints.messages.ScopePayload

case class GiveConsentProactive(
    scope: Set[ScopePayload]
)

object GiveConsentProactive {
  given Decoder[GiveConsentProactive] = unSnakeCaseIfy(
    deriveDecoder[GiveConsentProactive]
  )

  given Encoder[GiveConsentProactive] = snakeCaseIfy(
    deriveEncoder[GiveConsentProactive]
  )

  given Schema[GiveConsentProactive] =
    Schema.derived[GiveConsentProactive](using Configuration.default.withSnakeCaseMemberNames)

}
