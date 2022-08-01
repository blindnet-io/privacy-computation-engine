package io.blindnet.privacy
package api.endpoints.messages.privacyrequest

import java.time.Instant

import cats.effect.*
import cats.implicits.*
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

case class DemandResponse(
    responseId: String,
    date: Instant,
    requestedAction: Action,
    status: Status,
    answer: Json,
    message: Option[String],
    lang: Option[String],
    includes: Option[String],
    data: Option[String]
)

object DemandResponse {
  given Decoder[DemandResponse] = unSnakeCaseIfy(deriveDecoder[DemandResponse])
  given Encoder[DemandResponse] = snakeCaseIfy(deriveEncoder[DemandResponse])

  given Schema[DemandResponse] =
    Schema.derived[DemandResponse](using Configuration.default.withSnakeCaseMemberNames)

}
