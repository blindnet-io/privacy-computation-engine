package io.blindnet.pce
package api.endpoints.messages.bridge

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
import api.endpoints.messages.*

case class CompletedDemandPayload(
    id: UUID,
    action: Action,
    dataSubject: Option[DataSubjectPayload],
    requestDate: Instant,
    responseDate: Instant,
    status: Status
)

object CompletedDemandPayload {
  given Decoder[CompletedDemandPayload] = unSnakeCaseIfy(deriveDecoder[CompletedDemandPayload])
  given Encoder[CompletedDemandPayload] = snakeCaseIfy(deriveEncoder[CompletedDemandPayload])

  given Schema[CompletedDemandPayload] =
    Schema.derived[CompletedDemandPayload](using Configuration.default.withSnakeCaseMemberNames)

  def fromPrivCompletedDemand(d: CompletedDemand) = {
    CompletedDemandPayload(
      d.id,
      d.action,
      d.dataSubject.map(DataSubjectPayload.fromDataSubject),
      d.requestDate,
      d.responseDate,
      d.status
    )
  }

}
