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

case class ApproveDemandPayload(
    id: UUID,
    msg: Option[String],
    lang: Option[String]
)

object ApproveDemandPayload {
  given Decoder[ApproveDemandPayload] = unSnakeCaseIfy(deriveDecoder[ApproveDemandPayload])
  given Encoder[ApproveDemandPayload] = snakeCaseIfy(deriveEncoder[ApproveDemandPayload])

  given Schema[ApproveDemandPayload] =
    Schema.derived[ApproveDemandPayload](using Configuration.default.withSnakeCaseMemberNames)

}
