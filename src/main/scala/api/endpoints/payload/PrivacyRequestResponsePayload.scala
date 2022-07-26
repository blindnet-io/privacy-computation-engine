package io.blindnet.privacy
package api.endpoints.payload

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import model.vocabulary.terms.*
import java.time.Instant

case class PrivacyRequestResponsePayload(
    responseId: String,
    requestId: String,
    date: Instant,
    demands: List[DemandResponse]
)

given Encoder[PrivacyRequestResponsePayload] =
  Encoder.forProduct4("response_id", "request_id", "date", "demands")(
    r => (r.responseId, r.requestId, r.date, r.demands)
  )
