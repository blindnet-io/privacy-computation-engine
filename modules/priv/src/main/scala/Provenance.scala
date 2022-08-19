package io.blindnet.pce
package priv

import terms.*
import io.blindnet.pce.priv.util.parsing.*
import java.util.UUID
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class Provenance(
    id: UUID,
    provenance: ProvenanceTerms,
    system: String
)

object Provenance {
  given Decoder[Provenance] = unSnakeCaseIfy(deriveDecoder[Provenance])
  given Encoder[Provenance] = snakeCaseIfy(deriveEncoder[Provenance])

  given Schema[Provenance] =
    Schema.derived[Provenance](using Configuration.default.withSnakeCaseMemberNames)

}
