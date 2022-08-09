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

case class Restriction()

object Restriction {
  given Decoder[Restriction] = deriveDecoder[Restriction]
  given Encoder[Restriction] = deriveEncoder[Restriction]
}

case class PrivacyRequestDemand(
    id: String,
    action: Action,
    message: Option[String],
    language: Option[String],
    data: Option[List[String]],
    restrictions: Option[List[Restriction]]
)

object PrivacyRequestDemand {
  given Decoder[PrivacyRequestDemand] = deriveDecoder[PrivacyRequestDemand]
  given Encoder[PrivacyRequestDemand] = deriveEncoder[PrivacyRequestDemand]

  given Schema[PrivacyRequestDemand] = Schema.derived[PrivacyRequestDemand]

  def toPrivDemand(id: UUID, reqId: UUID, d: PrivacyRequestDemand) = {
    Demand(
      id,
      reqId,
      d.action,
      d.message,
      d.language,
      d.data.getOrElse(List.empty),
      // TODO: restrictions
      List.empty
    )
  }

}

case class CreatePrivacyRequestPayload(
    target: Option[Target],
    email: Option[String],
    demands: List[PrivacyRequestDemand],
    dataSubject: List[DataSubject]
)

object CreatePrivacyRequestPayload {
  given Decoder[CreatePrivacyRequestPayload] = unSnakeCaseIfy(
    deriveDecoder[CreatePrivacyRequestPayload]
  )

  given Encoder[CreatePrivacyRequestPayload] = snakeCaseIfy(
    deriveEncoder[CreatePrivacyRequestPayload]
  )

  given Schema[PrivacyRequestDemand] = Schema.derived[PrivacyRequestDemand]
}
