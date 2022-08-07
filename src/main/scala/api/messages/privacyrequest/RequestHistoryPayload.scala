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
import io.blindnet.privacy.model.vocabulary.request.PrivacyResponse
import sttp.tapir.Validator

enum PrStatus(val s: String) {
  case InProcessing       extends PrStatus("IN_PROCESSING")
  case PartiallyCompleted extends PrStatus("PARTIALLY_COMPLETED")
  case Completed          extends PrStatus("COMPLETED")
  case Canceled           extends PrStatus("CANCELED")
}

object PrStatus {
  given Encoder[PrStatus] =
    Encoder[String].contramap(_.s)

  given Schema[PrStatus] =
    Schema.string.validate(Validator.enumeration(PrStatus.values.toList, s => Some(s.s)))

}

case class PrItem(
    id: String,
    date: Instant,
    demands: Int,
    status: PrStatus
)

object PrItem {
  given Decoder[PrItem] = deriveDecoder[PrItem]
  given Encoder[PrItem] = deriveEncoder[PrItem]

  given Schema[PrItem] = Schema.derived[PrItem]
}

case class RequestHistoryPayload(
    history: List[PrItem]
)

object RequestHistoryPayload {
  given Decoder[RequestHistoryPayload] = deriveDecoder[RequestHistoryPayload]
  given Encoder[RequestHistoryPayload] = deriveEncoder[RequestHistoryPayload]

  given Schema[RequestHistoryPayload] = Schema.derived[RequestHistoryPayload]
}
