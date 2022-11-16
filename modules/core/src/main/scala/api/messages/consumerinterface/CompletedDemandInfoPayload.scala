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
import sttp.tapir.json.circe.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import api.endpoints.messages.*

case class CompletedDemandInfoPayload(
    demandId: UUID,
    responseId: UUID,
    requestDate: Instant,
    responseDate: Instant,
    action: Action,
    status: Status,
    motive: Option[Motive],
    answer: Option[String],
    requestMessage: Option[String],
    requestLang: Option[String],
    responseMessage: Option[String],
    responseLang: Option[String]
)

object CompletedDemandInfoPayload {
  given Decoder[CompletedDemandInfoPayload] = unSnakeCaseIfy(
    deriveDecoder[CompletedDemandInfoPayload]
  )

  given Encoder[CompletedDemandInfoPayload] = snakeCaseIfy(
    deriveEncoder[CompletedDemandInfoPayload]
  )

  given Schema[CompletedDemandInfoPayload] =
    Schema.derived[CompletedDemandInfoPayload](using Configuration.default.withSnakeCaseMemberNames)

  def fromPriv(d: Demand, req: PrivacyRequest, r: PrivacyResponse) = {
    CompletedDemandInfoPayload(
      d.id,
      r.id.value,
      req.timestamp,
      r.timestamp,
      r.action,
      r.status,
      r.motive,
      r.answer,
      d.message,
      d.language,
      r.message,
      r.lang
    )
  }

}
