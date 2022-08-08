package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*
import model.vocabulary.terms.DataCategory
import java.time.Instant
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.Configuration
import io.blindnet.privacy.util.parsing.*

case class Recommendation(
    id: String,
    dId: String,
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
