package io.blindnet.pce
package api.endpoints.messages.configuration

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.Configuration

case class CreateProvenancePayload(
    dataCategory: DataCategory,
    provenance: ProvenanceTerms,
    system: Option[String]
)

object CreateProvenancePayload {
  given Decoder[CreateProvenancePayload] = unSnakeCaseIfy(deriveDecoder[CreateProvenancePayload])
  given Encoder[CreateProvenancePayload] = snakeCaseIfy(deriveEncoder[CreateProvenancePayload])

  given Schema[CreateProvenancePayload] =
    Schema.derived[CreateProvenancePayload](using Configuration.default.withSnakeCaseMemberNames)

}
