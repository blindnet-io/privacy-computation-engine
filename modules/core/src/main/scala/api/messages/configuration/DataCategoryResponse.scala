package io.blindnet.pce
package api.endpoints.messages.configuration

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

case class DataCategoryResponsePayload(
    dataCategory: DataCategory,
    provenances: List[Provenance],
    retentionPolicies: List[RetentionPolicy]
)

object DataCategoryResponsePayload {
  given Decoder[DataCategoryResponsePayload] = unSnakeCaseIfy(
    deriveDecoder[DataCategoryResponsePayload]
  )

  given Encoder[DataCategoryResponsePayload] = snakeCaseIfy(
    deriveEncoder[DataCategoryResponsePayload]
  )

  given Schema[DataCategoryResponsePayload] =
    Schema.derived[DataCategoryResponsePayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

}
