package io.blindnet.pce
package priv
package privacyrequest

import PrivacyScope as PS
import terms.*
import java.time.Instant
import doobie.util.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import java.util.UUID

enum Restriction {
  case PrivacyScope(scope: PS) extends Restriction

  case Consent(consentId: UUID) extends Restriction

  case DateRange(from: Option[Instant], to: Option[Instant]) extends Restriction

  case Provenance(term: ProvenanceTerms, target: Option[Target]) extends Restriction

  case DataReference(dataReferences: List[String]) extends Restriction
}

object Restriction {

  type ConsentId   = Option[UUID]
  type FromDate    = Option[Instant]
  type ToDate      = Option[Instant]
  type ProvenanceT = Option[ProvenanceTerms]
  type TargetT     = Option[Target]
  type DataRef     = Option[List[String]]

  given Read[Restriction] =
    Read[(String, ConsentId, FromDate, ToDate, ProvenanceT, TargetT, DataRef)].map {
      case ("PRIVACY_SCOPE", _, _, _, _, _, _)         => PrivacyScope(PS.empty)
      case ("CONSENT", Some(cId), _, _, _, _, _)       => Consent(cId)
      case ("DATE_RANGE", _, from, to, _, _, _)        => DateRange(from, to)
      case ("PROVENANCE", _, _, _, Some(p), t, _)      => Provenance(p, t)
      case ("DATA_REFERENCE", _, _, _, _, _, Some(dr)) => DataReference(dr)
      // TODO
      case _                                           => throw new NotImplementedError()
    }

}
