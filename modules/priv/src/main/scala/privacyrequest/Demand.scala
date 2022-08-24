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

  def hasValidRestrictions = {
    lazy val hasConsentR        = restrictions.exists(r => r.isInstanceOf[Restriction.Consent])
    // Consent Restriction with any other type of Restriction
    lazy val consentRWithOthers = hasConsentR && restrictions.length > 1
    // Consent Restriction within a Demand other than REVOKE-CONSENT
    lazy val consentRNoRevoke   = hasConsentR && action != Action.RevokeConsent
    // More than one Data Reference Restriction
    lazy val moreOneDrefR   = restrictions.count(r => r.isInstanceOf[Restriction.DataReference]) > 1
    // More than one Date Range Restriction
    lazy val moreOneDrangeR = restrictions.count(r => r.isInstanceOf[Restriction.DateRange]) > 1

    !consentRWithOthers &&
    !consentRNoRevoke &&
    !moreOneDrefR &&
    !moreOneDrangeR
  }

}

object Demand {
  def validate(demand: Demand): ValidatedNel[String, Demand] = {
    demand.valid
  }

}
