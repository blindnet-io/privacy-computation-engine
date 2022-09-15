package io.blindnet.pce
package priv

import java.util.UUID

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import terms.*

case class Provenance(
    id: UUID,
    provenance: ProvenanceTerms,
    system: Option[String]
)

object Provenance {
  given Decoder[Provenance] = unSnakeCaseIfy(deriveDecoder[Provenance])
  given Encoder[Provenance] = snakeCaseIfy(deriveEncoder[Provenance])

  given Schema[Provenance] =
    Schema.derived[Provenance](using Configuration.default.withSnakeCaseMemberNames)

}
