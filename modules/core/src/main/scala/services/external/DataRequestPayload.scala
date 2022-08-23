package io.blindnet.pce
package services.external

import java.time.Instant

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.blindnet.pce.util.parsing.*

enum DataRequestAction {
  case GET, DELETE
}

object DataRequestAction {
  given Encoder[DataRequestAction] = Encoder.encodeString.contramap(_.toString.toLowerCase)
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
    //  app_id: String,
    request_id: String,
    query: DataQueryPayload,
    action: DataRequestAction,
    callback: String
)

object DataRequestPayload {
  given Decoder[DataRequestPayload] = deriveDecoder[DataRequestPayload]
  given Encoder[DataRequestPayload] = deriveEncoder[DataRequestPayload]
}
