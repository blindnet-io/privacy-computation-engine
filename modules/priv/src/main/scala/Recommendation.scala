package io.blindnet.pce
package priv

import java.time.Instant
import java.util.UUID

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import terms.*
import cats.data.Validated
import cats.implicits.*

case class Recommendation(
    id: UUID,
    dId: UUID,
    status: Option[Status],
    motive: Option[Motive] = None,
    dataCategories: Set[DataCategory] = Set.empty,
    dateFrom: Option[Instant] = None,
    dateTo: Option[Instant] = None,
    provenance: Option[ProvenanceTerms] = None,
    target: Option[Target] = None
)

object Recommendation {

  def grant(id: UUID, dId: UUID) =
    Recommendation(id, dId, Some(Status.Granted))

  def rejectReqUnsupported(id: UUID, dId: UUID) =
    Recommendation(id, dId, Some(Status.Denied), Some(Motive.RequestUnsupported))

  def rejectIdentityNotProvided(id: UUID, dId: UUID) =
    Recommendation(id, dId, Some(Status.Denied), Some(Motive.IdentityUnconfirmed))

  def rejectUnknownIdentity(id: UUID, dId: UUID) =
    Recommendation(id, dId, Some(Status.Denied), Some(Motive.UserUnknown))

  def validate(r: Recommendation): Validated[String, Recommendation] = {
    if r.status == Some(Status.Denied) && r.motive == None then
      "Motive has to be set for denied requests".invalid
    else r.valid
  }

  given Decoder[Recommendation] = unSnakeCaseIfy(deriveDecoder[Recommendation])
  given Encoder[Recommendation] = snakeCaseIfy(deriveEncoder[Recommendation])

  given Schema[Recommendation] =
    Schema.derived[Recommendation](using Configuration.default.withSnakeCaseMemberNames)

}
