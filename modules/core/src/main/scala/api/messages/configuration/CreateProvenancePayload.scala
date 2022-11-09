package io.blindnet.pce
package api.endpoints.messages.configuration

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*

@description("origin of the data category")
case class CreateProvenancePayload(
    @description("data category for which the provenance is created")
    @encodedExample("CONTACT.PHONE")
    dataCategory: DataCategory,
    @description("provenance type")
    @encodedExample("USER")
    provenance: ProvenanceTerms,
    @description("id of the system data category originated from. null for own system")
    @encodedExample("https://blindnet.io")
    system: String
)

object CreateProvenancePayload {
  given Decoder[CreateProvenancePayload] = unSnakeCaseIfy(deriveDecoder[CreateProvenancePayload])
  given Encoder[CreateProvenancePayload] = snakeCaseIfy(deriveEncoder[CreateProvenancePayload])

  given Schema[CreateProvenancePayload] =
    Schema.derived[CreateProvenancePayload](using Configuration.default.withSnakeCaseMemberNames)

}
