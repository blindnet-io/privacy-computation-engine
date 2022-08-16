package io.blindnet.pce
package api.endpoints.messages.privacyrequest

import java.util.UUID

import cats.effect.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.{
  deriveDecoder as semiDeriveDecoder,
  deriveEncoder as semiDeriveEncoder
}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import java.time.Instant
import sttp.tapir.generic.Configuration

// TODO: privacy scope
case class PrivacyScopeRestriction()
case class ConsentRestriction(id: UUID)
case class DateRangeRestriction(from: Option[Instant], to: Option[Instant])
case class ProvenanceRestriction(term: ProvenanceTerms, target: Option[Target])
case class DataReferenceRestriction(ref: List[String])

case class Restrictions(
    privacyScope: Option[PrivacyScopeRestriction],
    consent: Option[ConsentRestriction],
    dateRange: Option[DateRangeRestriction],
    provenance: Option[ProvenanceRestriction],
    dataReference: Option[DataReferenceRestriction]
)

object Restrictions {
  given Decoder[Restrictions] = unSnakeCaseIfy(semiDeriveDecoder[Restrictions])
  given Encoder[Restrictions] = snakeCaseIfy(semiDeriveEncoder[Restrictions])

  given Schema[Restrictions] =
    Schema.derived[Restrictions](using Configuration.default.withSnakeCaseMemberNames)

  def toPrivRestrictions(r: Restrictions): List[Restriction] =
    List(
      // TODO: privacy scope
      r.privacyScope.map(ps => Restriction.PrivacyScope(PrivacyScope.empty)),
      r.consent.map(c => Restriction.Consent(c.id)),
      r.dateRange.map(dr => Restriction.DateRange(dr.from, dr.to)),
      r.provenance.map(p => Restriction.Provenance(p.term, p.target)),
      r.dataReference.map(dr => Restriction.DataReference(dr.ref))
    ).flatten

}

case class PrivacyRequestDemand(
    id: String,
    action: Action,
    message: Option[String],
    language: Option[String],
    data: Option[List[String]],
    restrictions: Option[Restrictions]
)

object PrivacyRequestDemand {
  given Decoder[PrivacyRequestDemand] = unSnakeCaseIfy(semiDeriveDecoder[PrivacyRequestDemand])
  given Encoder[PrivacyRequestDemand] = snakeCaseIfy(semiDeriveEncoder[PrivacyRequestDemand])

  given Schema[PrivacyRequestDemand] = Schema.derived[PrivacyRequestDemand]

  def toPrivDemand(id: UUID, reqId: UUID, d: PrivacyRequestDemand) = {
    Demand(
      id,
      reqId,
      d.action,
      d.message,
      d.language,
      d.data.getOrElse(List.empty),
      d.restrictions.map(Restrictions.toPrivRestrictions).getOrElse(List.empty)
    )
  }

}

case class CreatePrivacyRequestPayload(
    target: Option[Target],
    email: Option[String],
    demands: List[PrivacyRequestDemand],
    dataSubject: List[DataSubject]
)

object CreatePrivacyRequestPayload {
  given Decoder[CreatePrivacyRequestPayload] = unSnakeCaseIfy(
    semiDeriveDecoder[CreatePrivacyRequestPayload]
  )

  given Encoder[CreatePrivacyRequestPayload] = snakeCaseIfy(
    semiDeriveEncoder[CreatePrivacyRequestPayload]
  )

  given Schema[PrivacyRequestDemand] = Schema.derived[PrivacyRequestDemand]
}
