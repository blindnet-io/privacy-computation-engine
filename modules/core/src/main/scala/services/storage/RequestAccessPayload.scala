package io.blindnet.pce
package services.storage

import java.time.Instant

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.blindnet.pce.util.parsing.*

case class RequestAccessPayload(
    selectors: List[String],
    subjects: List[String],
    provenance: Option[String],
    target: Option[String],
    after: Option[Instant],
    until: Option[Instant],
    appId: String,
    requestId: String,
    callback: String
)

object RequestAccessPayload {
  given Decoder[RequestAccessPayload] = unSnakeCaseIfy(deriveDecoder[RequestAccessPayload])
  given Encoder[RequestAccessPayload] = snakeCaseIfy(deriveEncoder[RequestAccessPayload])
}
