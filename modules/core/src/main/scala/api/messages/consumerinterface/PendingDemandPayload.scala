package io.blindnet.pce
package api.endpoints.messages.consumerinterface

import java.time.Instant
import java.util.UUID

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class PendingDemandPayload(
    id: UUID,
    date: Instant,
    action: Action,
    dataSubject: List[DataSubject]
)

object PendingDemandPayload {
  given Decoder[PendingDemandPayload] = unSnakeCaseIfy(deriveDecoder[PendingDemandPayload])
  given Encoder[PendingDemandPayload] = snakeCaseIfy(deriveEncoder[PendingDemandPayload])

  given Schema[PendingDemandPayload] =
    Schema.derived[PendingDemandPayload](using Configuration.default.withSnakeCaseMemberNames)

  def fromPrivDemand(d: Demand, pr: PrivacyRequest) = {
    PendingDemandPayload(d.id, pr.timestamp, d.action, pr.dataSubject)
  }

}
