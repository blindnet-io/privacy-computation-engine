package io.blindnet.pce
package requesthandlers.recommender

import java.util.UUID

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.model.error.*
import priv.Recommendation
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.util.extension.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import db.repositories.Repositories
import io.blindnet.pce.api.endpoints.messages.privacyrequest.DateRangeRestriction
import io.blindnet.pce.model.*
import io.blindnet.pce.priv.*

object Validations {
  import priv.terms.Action.*
  import priv.terms.Status.*

  def validateDataSubject(pr: PrivacyRequest, d: Demand): Either[UUID => Recommendation, Unit] =
    d.action match {
      case a if a == Transparency || a.isChildOf(Transparency) => ().asRight
      case Other                                               => ().asRight
      // TODO: this is flawed
      // pr.providedDsIds are provided data subjects in the request
      // pr.dataSubject is None if a data subject is not known by the PCE but authenticated by the system
      case _                                                   =>
        (pr.dataSubject, pr.providedDsIds) match {
          // identity not provided
          case (None, Nil)   =>
            ((id: UUID) => Recommendation.rejectIdentityNotProvided(id, d.id)).asLeft

          // unknown identity
          case (None, _)     =>
            ((id: UUID) => Recommendation.rejectUnknownIdentity(id, d.id)).asLeft

          // known identity
          case (Some(ds), _) => ().asRight
        }
    }

  def validateRestrictions(d: Demand): Either[UUID => Recommendation, Unit] = {
    lazy val hasConsentR   = d.restrictions.exists(r => r.isInstanceOf[Restriction.Consent])
    lazy val hasPrivScopeR = d.restrictions.exists(r => r.isInstanceOf[Restriction.PrivacyScope])

    val validCommonRules = {
      // Consent Restriction with any other type of Restriction
      lazy val consentRWithOthers = hasConsentR && d.restrictions.length > 1
      // Consent Restriction within a Demand other than REVOKE-CONSENT
      lazy val consentRNoRevoke   = hasConsentR && d.action != Action.RevokeConsent
      // More than one Data Reference Restriction
      lazy val moreOneDrefR       =
        d.restrictions.count(r => r.isInstanceOf[Restriction.DataReference]) > 1
      // More than one Date Range Restriction
      lazy val moreOneDrangeR = d.restrictions.count(r => r.isInstanceOf[Restriction.DateRange]) > 1

      !consentRWithOthers &&
      !consentRNoRevoke &&
      !moreOneDrefR &&
      !moreOneDrangeR
    }

    // privacy scope contains processing category or purpose
    lazy val validPS = d.restrictions
      .find(r => r.isInstanceOf[Restriction.PrivacyScope])
      .map(r => r.asInstanceOf[Restriction.PrivacyScope])
      .map(
        r =>
          !r.scope.triples.exists(_.processingCategory.term != "*") ||
            !r.scope.triples.exists(_.purpose.term != "*")
      )
      .getOrElse(true)

    val validForAction = d.action match {
      case RevokeConsent     => hasConsentR
      case Object | Restrict => hasPrivScopeR
      case Delete | Modify   => validPS
      case _                 => true
    }

    if validCommonRules && validForAction then ().asRight
    else ((id: UUID) => Recommendation.rejectReqUnsupported(id, d.id)).asLeft
  }

  def validate(pr: PrivacyRequest, d: Demand) =
    validateDataSubject(pr, d) *> validateRestrictions(d)

}
