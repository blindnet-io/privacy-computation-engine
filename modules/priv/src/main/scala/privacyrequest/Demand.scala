package io.blindnet.pce
package priv
package privacyrequest

import java.util.UUID

import cats.data.*
import cats.implicits.*
import terms.*

case class Demand(
    id: UUID,
    reqId: UUID,
    action: Action,
    message: Option[String],
    // TODO: parse https://datatracker.ietf.org/doc/rfc5646/
    language: Option[String],
    // TODO: what format?
    data: List[String],
    restrictions: List[Restriction]
) {
  def getPSR = restrictions.flatMap {
    case Restriction.PrivacyScope(ps) => Option((ps))
    case _                            => None
  }.headOption

  def getDateRangeR = restrictions.flatMap {
    case Restriction.DateRange(from, to) => Option((from, to))
    case _                               => None
  }.headOption

  def getProvenanceR = restrictions.flatMap {
    case Restriction.Provenance(p, t) => Option((p, t))
    case _                            => None
  }.headOption

  def getDataRefR = restrictions.flatMap {
    case Restriction.DataReference(dr) => Option((dr))
    case _                             => None
  }.headOption

}

object Demand {
  def validate(demand: Demand): ValidatedNel[String, Demand] = {
    // TODO: validate restrictions
    demand.valid
  }

}
