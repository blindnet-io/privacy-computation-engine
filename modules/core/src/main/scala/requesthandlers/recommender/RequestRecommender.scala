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
import io.blindnet.pce.model.DemandToRespond
import io.blindnet.pce.model.PCEApp
import io.blindnet.pce.priv.PrivacyScope
import io.blindnet.pce.priv.PrivacyScopeTriple

object Validations {
  import priv.terms.Action.*
  import priv.terms.Status.*

  def validateDataSubject(pr: PrivacyRequest, d: Demand): Either[UUID => Recommendation, Unit] =
    d.action match {
      case a if a == Transparency || a.isChildOf(Transparency) => ().asRight
      case Other                                               => ().asRight
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

class RequestRecommender(
    repos: Repositories
) {
  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def processDemand(dId: UUID): IO[Unit] =
    for {
      // TODO .get
      resp <- repos.privacyRequest.getDemandResponse(dId).map(_.get)
      _    <- resp.status match {
        case UnderReview =>
          for {
            d   <- repos.privacyRequest.getDemand(dId, true).map(_.get)
            pr  <- repos.privacyRequest.getRequest(d).map(_.get)
            app <- repos.app.get(pr.appId).map(_.get)
            _   <- validateDemand(app, pr, d)
          } yield ()
        case _           => logger.info(s"Demand $dId not UNDER-REVIEW")
      }
    } yield ()

  def storeRecommendation(f: UUID => Recommendation) =
    UUIDGen[IO].randomUUID map f >>= repos.privacyRequest.storeRecommendation

  private def validateDemand(app: PCEApp, pr: PrivacyRequest, d: Demand): IO[Unit] =
    Validations
      .validate(pr, d)
      .fold(
        rf => storeRecommendation(rf) *> repos.demandsToRespond.add(List(DemandToRespond(d.id))),
        _ => handleRecommendation(app, pr, d)
      )

  private def handleRecommendation(app: PCEApp, pr: PrivacyRequest, d: Demand) =
    for {
      recOpt <- repos.privacyRequest.getRecommendation(d.id)
      _      <- recOpt match {
        case None => getRecommendation(pr, d) >>= storeRecommendation
        // recommendation exists
        case _    => IO.unit
      }
      _      <- storeDemandForNextStep(app, d)
    } yield ()

  private def getRecommendation(pr: PrivacyRequest, d: Demand) =
    d.action match {
      case a if a == Transparency || a.isChildOf(Transparency) => IO(Recommendation.grant(_, d.id))
      case Access | Delete                                     => getRec(pr, d)
      case RevokeConsent                                       => getRecRevoke(pr, d)
      case _ => IO.raiseError(new NotImplementedError)
    }

  private def getRec(pr: PrivacyRequest, d: Demand) =
    for {
      timeline <- repos.events.getTimeline(pr.appId, pr.dataSubject.get)
      eps = timeline.eligiblePrivacyScope(Some(pr.timestamp))

      psr = d.getPSR.orEmpty
      psRec <-
        if psr.isEmpty then IO(eps)
        else
          repos.privacyScope
            .getSelectors(pr.appId, active = true)
            .map(psr.zoomIn)
            .map(ps => ps intersection eps)

      drRec = d.getDateRangeR.getOrElse((None, None))
      pRec  = d.getProvenanceR.map(_._1)
      // TODO: data reference restriction

      dcs = psRec.triples.map(_.dataCategory)
      r   = Recommendation(_, d.id, Some(Status.Granted), None, dcs, drRec._1, drRec._2, pRec)
    } yield r

  private def getRecRevoke(pr: PrivacyRequest, d: Demand) =
    IO(d.restrictions.head.asInstanceOf[Restriction.Consent].consentId)
      .flatMap(
        cId =>
          for {
            lbOpt <- repos.legalBase.get(pr.appId, cId, false)
            isConsent = lbOpt.map(_.isConsent).getOrElse(false)
            rec       =
              if isConsent then Recommendation.grant(_, d.id)
              else Recommendation.rejectReqUnsupported(_, d.id)
          } yield rec
      )
      .handleError(_ => Recommendation.rejectReqUnsupported(_, d.id))

  private def storeDemandForNextStep(app: PCEApp, d: Demand) =
    val auto: Boolean =
      d.action match {
        case a if a == Transparency || a.isChildOf(Transparency) => app.autoResolve.transparency
        case Access                                              => app.autoResolve.access
        case Delete                                              => app.autoResolve.delete
        case RevokeConsent                                       => app.autoResolve.consents
        case _                                                   => false
      }

    if auto then repos.demandsToRespond.add(List(DemandToRespond(d.id)))
    else repos.demandsToReview.add(List(d.id))

}

object RequestRecommender {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new RequestRecommender(repos)

    def loop(): IO[Unit] =
      for {
        ids <- repos.demandsToProcess.get()
        _   <- ids.parTraverse_(
          id => {
            val p = for {
              _ <- logger.info(s"Processing new demand $id")
              _ <- repos.demandsToProcess.remove(NonEmptyList.one(id))
              _ <- reqProc.processDemand(id)
              _ <- logger.info(s"Demand $id processed")
            } yield ()

            p.handleErrorWith(
              e =>
                logger
                  .error(e)(s"Error processing demand $id - ${e.getMessage}")
                  .flatMap(_ => repos.demandsToProcess.add(List(id)))
            )
          }
        )

        _ <- IO.sleep(1.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
