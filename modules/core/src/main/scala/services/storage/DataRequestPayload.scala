package io.blindnet.pce
package services.storage

import java.time.Instant

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.blindnet.pce.util.parsing.*

import io.circe.{ Decoder, Encoder }

object DataRequestActions extends Enumeration {
  type DataRequestAction = Value

  val Get, Delete = Value

  private val byLowerName = DataRequestActions.values.map(e => (e.toString.toLowerCase, e)).toMap
  implicit val decoder: Decoder[DataRequestActions.DataRequestAction] =
    Decoder.decodeString.emap[DataRequestActions.DataRequestAction](
      k => byLowerName.get(k.toLowerCase).toRight("illegal DataRequestAction value")
    )

  implicit val encoder: Encoder[DataRequestActions.DataRequestAction] =
    Encoder.encodeString.contramap(_.toString.toLowerCase)

}

case class DataQueryPayload(
    selectors: List[String],
    subjects: List[String],
    provenance: Option[String],
    target: Option[String],
    after: Option[Instant],
    until: Option[Instant]
)

case class DataRequestPayload(
    request_id: String,
    query: DataQueryPayload,
    action: DataRequestActions.DataRequestAction,
    callback: String
)

object DataRequestPayload {
  given Decoder[DataRequestPayload] = deriveDecoder[DataRequestPayload]
  given Encoder[DataRequestPayload] = deriveEncoder[DataRequestPayload]
}
