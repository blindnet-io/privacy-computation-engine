package io.blindnet.pce
package priv

import io.circe.*
import io.circe.generic.semiauto.*
import java.util.UUID

case class DataSubject(
    id: String,
    appId: UUID,
    schema: Option[String] = None
)

object DataSubject {
  given Decoder[DataSubject] = deriveDecoder[DataSubject]
  given Encoder[DataSubject] = deriveEncoder[DataSubject]
}
