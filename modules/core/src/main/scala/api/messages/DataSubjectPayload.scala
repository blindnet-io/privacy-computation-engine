package io.blindnet.pce
package api.endpoints.messages

import java.util.UUID

import sttp.tapir.generic.Configuration

import io.circe.*
import io.circe.generic.semiauto.*
import java.util.UUID
import priv.*
import sttp.tapir.Schema

case class DataSubjectPayload(
    id: String,
    schema: Option[String] = None
) {
  def toPrivDataSubject(appId: UUID) = DataSubject(id, appId, schema)
}

object DataSubjectPayload {
  def fromDataSubject(ds: DataSubject) = DataSubjectPayload(ds.id, ds.schema)

  given Decoder[DataSubjectPayload] = deriveDecoder[DataSubjectPayload]
  given Encoder[DataSubjectPayload] = deriveEncoder[DataSubjectPayload]

  given Schema[DataSubjectPayload] = Schema.derived[DataSubjectPayload]
}
