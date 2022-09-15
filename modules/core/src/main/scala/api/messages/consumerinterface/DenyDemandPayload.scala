package io.blindnet.pce
package api.endpoints.messages.consumerinterface

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*

case class DenyDemandPayload(
    id: UUID,
    motive: Motive,
    msg: Option[String],
    lang: Option[String]
)

object DenyDemandPayload {
  given Decoder[DenyDemandPayload] = unSnakeCaseIfy(deriveDecoder[DenyDemandPayload])
  given Encoder[DenyDemandPayload] = snakeCaseIfy(deriveEncoder[DenyDemandPayload])

  given Schema[DenyDemandPayload] =
    Schema.derived[DenyDemandPayload](using Configuration.default.withSnakeCaseMemberNames)

}
