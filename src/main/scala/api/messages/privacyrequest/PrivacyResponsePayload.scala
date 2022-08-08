package io.blindnet.privacy
package api.endpoints.messages.privacyrequest

import java.time.Instant
import java.util.UUID

import cats.effect.*
import cats.implicits.*
import io.blindnet.privacy.model.vocabulary.request.PrivacyResponse
import io.blindnet.privacy.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.circe.*
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import model.vocabulary.terms.*

case class PrivacyResponsePayload(
    demandId: UUID,
    date: Instant,
    requestedAction: Action,
    status: Status,
    answer: Option[Json],
    message: Option[String],
    lang: Option[String],
    includes: List[String], // TODO: recursive type
    data: Option[String]
)

object PrivacyResponsePayload {
  given Decoder[PrivacyResponsePayload] = unSnakeCaseIfy(deriveDecoder[PrivacyResponsePayload])
  given Encoder[PrivacyResponsePayload] = snakeCaseIfy(deriveEncoder[PrivacyResponsePayload])

  given Schema[PrivacyResponsePayload] =
    Schema.derived[PrivacyResponsePayload](using Configuration.default.withSnakeCaseMemberNames)

  def fromPrivPrivacyResponse(pr: PrivacyResponse): PrivacyResponsePayload = {
    PrivacyResponsePayload(
      pr.demandId,
      pr.timestamp,
      pr.action,
      pr.status,
      pr.answer,
      pr.message,
      pr.lang,
      // pr.includes.map(PrivacyResponsePayload.fromPrivPrivacyResponse),
      pr.includes.map(_ => ""),
      pr.data
    )
  }

}
