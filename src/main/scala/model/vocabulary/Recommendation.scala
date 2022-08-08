package io.blindnet.privacy
package model.vocabulary

import java.time.Instant
import java.util.UUID

import io.blindnet.privacy.model.vocabulary.terms.{ DataCategory, * }
import io.blindnet.privacy.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*

case class Recommendation(
    id: UUID,
    dId: UUID,
    dataCategories: Set[DataCategory],
    dateFrom: Option[Instant],
    dateTo: Option[Instant],
    provenance: Option[ProvenanceTerms]
)

object Recommendation {
  given Decoder[Recommendation] = unSnakeCaseIfy(deriveDecoder[Recommendation])
  given Encoder[Recommendation] = snakeCaseIfy(deriveEncoder[Recommendation])

  given Schema[Recommendation] =
    Schema.derived[Recommendation](using Configuration.default.withSnakeCaseMemberNames)

}
