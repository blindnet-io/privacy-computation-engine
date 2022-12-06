package io.blindnet.pce
package api.endpoints.messages.bridge

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*

case class RecommendationPayload(
    status: Status,
    motive: Option[Motive],
    dataCategories: Option[Set[DataCategory]],
    dateFrom: Option[Instant],
    dateTo: Option[Instant],
    provenance: Option[ProvenanceTerms],
    target: Option[Target]
)

object RecommendationPayload {
  given Decoder[RecommendationPayload] = unSnakeCaseIfy(deriveDecoder[RecommendationPayload])
  given Encoder[RecommendationPayload] = snakeCaseIfy(deriveEncoder[RecommendationPayload])

  given Schema[RecommendationPayload] =
    Schema.derived[RecommendationPayload](using Configuration.default.withSnakeCaseMemberNames)

  def toRecommendation(rp: RecommendationPayload, dId: UUID) =
    Recommendation(
      null,
      dId,
      Some(rp.status),
      rp.motive,
      rp.dataCategories.getOrElse(Set.empty),
      rp.dateFrom,
      rp.dateTo,
      rp.provenance,
      rp.target
    )

}

case class ChangeRecommendationPayload(
    demandId: UUID,
    recommendation: RecommendationPayload
)

object ChangeRecommendationPayload {
  given Decoder[ChangeRecommendationPayload] = unSnakeCaseIfy(
    deriveDecoder[ChangeRecommendationPayload]
  )

  given Encoder[ChangeRecommendationPayload] = snakeCaseIfy(
    deriveEncoder[ChangeRecommendationPayload]
  )

  given Schema[ChangeRecommendationPayload] =
    Schema.derived[ChangeRecommendationPayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

}
