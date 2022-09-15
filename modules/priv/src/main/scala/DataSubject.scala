package io.blindnet.pce
package priv

import java.util.UUID

import io.circe.*
import io.circe.generic.semiauto.*

case class DataSubject(
    id: String,
    appId: UUID,
    schema: Option[String] = None
)

object DataSubject {
  given Decoder[DataSubject] = deriveDecoder[DataSubject]
  given Encoder[DataSubject] = deriveEncoder[DataSubject]
}
