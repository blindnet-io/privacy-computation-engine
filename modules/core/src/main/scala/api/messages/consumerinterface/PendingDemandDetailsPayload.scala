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

case class PendingDemandDetailsPayload(
    id: UUID,
    date: Instant,
    action: Action,
    dataSubject: Option[DataSubject],
    recommendation: Option[Recommendation]
)

object PendingDemandDetailsPayload {
  given Decoder[PendingDemandDetailsPayload] = unSnakeCaseIfy(
    deriveDecoder[PendingDemandDetailsPayload]
  )

  given Encoder[PendingDemandDetailsPayload] = snakeCaseIfy(
    deriveEncoder[PendingDemandDetailsPayload]
  )

  given Schema[PendingDemandDetailsPayload] =
    Schema.derived[PendingDemandDetailsPayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

  def fromPrivDemand(d: Demand, pr: PrivacyRequest, r: Option[Recommendation]) = {
    PendingDemandDetailsPayload(d.id, pr.timestamp, d.action, pr.dataSubject, r)
  }

}
