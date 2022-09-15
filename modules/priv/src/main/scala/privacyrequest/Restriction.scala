package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID

import scala.reflect.TypeTest

import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.*
import priv.PrivacyScope as PS
import terms.*

type Tag[T] = TypeTest[Any, T]

enum Restriction {
  case PrivacyScope(scope: PS) extends Restriction

  case Consent(consentId: UUID) extends Restriction

  case DateRange(from: Option[Instant], to: Option[Instant]) extends Restriction

  case Provenance(term: ProvenanceTerms, target: Option[Target]) extends Restriction

  case DataReference(dataReferences: List[String]) extends Restriction

  def cast[T <: Restriction: Tag]: Option[T] =
    this match {
      case ps: T => Some(ps)
      case _     => None
    }

}

object Restriction {

  type ConsentId   = Option[UUID]
  type FromDate    = Option[Instant]
  type ToDate      = Option[Instant]
  type ProvenanceT = Option[ProvenanceTerms]
  type TargetT     = Option[Target]
  type DataRef     = Option[List[String]]
  type DCs         = List[Option[String]]
  type PCs         = List[Option[String]]
  type PPs         = List[Option[String]]

  given Read[Restriction] =
    Read[(String, ConsentId, FromDate, ToDate, ProvenanceT, TargetT, DataRef, DCs, PCs, PPs)].map {
      case ("PRIVACY_SCOPE", _, _, _, _, _, _, dcs, pcs, pps)   =>
        PrivacyScope(PS.unsafe(dcs.flatten, pcs.flatten, pps.flatten))
      case ("CONSENT", Some(cId), _, _, _, _, _, _, _, _)       => Consent(cId)
      case ("DATE_RANGE", _, from, to, _, _, _, _, _, _)        => DateRange(from, to)
      case ("PROVENANCE", _, _, _, Some(p), t, _, _, _, _)      => Provenance(p, t)
      case ("DATA_REFERENCE", _, _, _, _, _, Some(dr), _, _, _) => DataReference(dr)
      // TODO
      case _                                                    => throw new NotImplementedError()
    }

  def cast[T <: Restriction: Tag](l: List[Restriction]): List[T] = l.flatMap(_.cast)

}
