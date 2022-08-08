package io.blindnet.privacy
package api.endpoints.messages.consumerinterface

import java.time.Instant

import cats.effect.*
import io.blindnet.privacy.model.vocabulary.*
import io.blindnet.privacy.model.vocabulary.request.*
import io.blindnet.privacy.model.vocabulary.terms.*
import io.blindnet.privacy.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class PendingDemandPayload(
    id: String,
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
