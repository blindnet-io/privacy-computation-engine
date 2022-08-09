package io.blindnet.pce
package priv

import io.circe.*
import io.circe.generic.semiauto.*

case class DataSubject(
    id: String,
    schema: String
)

object DataSubject {
  given Decoder[DataSubject] = deriveDecoder[DataSubject]
  given Encoder[DataSubject] = deriveEncoder[DataSubject]
}
