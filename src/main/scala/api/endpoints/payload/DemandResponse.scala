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

case class DemandResponse(
    responseId: String,
    demandId: String,
    date: Instant,
    requestedAction: Action,
    status: String, // TODO: status terms
    answer: Json,
    message: Option[String],
    lang: String,
    includes: Option[String],
    data: Option[String]
)

given Encoder[DemandResponse] = Encoder.forProduct10(
  "response_id",
  "demand_id",
  "date",
  "action",
  "status",
  "answer",
  "message",
  "lang",
  "includes",
  "data"
)(
  r =>
    (
      r.responseId,
      r.demandId,
      r.date,
      r.requestedAction,
      r.status,
      r.answer,
      r.message,
      r.lang,
      r.includes,
      r.data
    )
)
