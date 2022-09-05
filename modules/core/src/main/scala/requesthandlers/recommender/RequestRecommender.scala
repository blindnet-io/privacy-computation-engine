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

class RequestRecommender(
    repos: Repositories
) {
  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def processDemand(c: CommandCreateRecommendation): IO[Unit] =
    for {
      d         <- repos.privacyRequest.getDemand(c.dId, true).map(_.get)
      pr        <- repos.privacyRequest.getRequest(d).map(_.get)
      app       <- repos.app.get(pr.appId).map(_.get)
      responses <- repos.privacyRequest.getDemandResponses(c.dId)
      _         <- responses.traverse(r => processResponse(app, pr, d, c, r))
      _         <- complete(app, d)
    } yield ()

  // TODO .get
  private def processResponse(
      app: PCEApp,
      pr: PrivacyRequest,
      d: Demand,
      c: CommandCreateRecommendation,
      resp: PrivacyResponse
  ): IO[Unit] =
    resp.status match {
      case UnderReview => validateDemand(app, pr, d)
      case _           => logger.info(s"Response ${resp.id} not UNDER-REVIEW")
    }

  def storeRecommendation(f: UUID => Recommendation) =
    UUIDGen[IO].randomUUID map f >>= repos.privacyRequest.storeRecommendation

  private def validateDemand(app: PCEApp, pr: PrivacyRequest, d: Demand): IO[Unit] =
    Validations
      .validate(pr, d)
      .fold(
        rf =>
          storeRecommendation(rf) >>
            CommandCreateResponse.create(d.id) map (c => List(c)) >>= repos.commands.addCreateResp,
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
    } yield ()

  private def getRecommendation(pr: PrivacyRequest, d: Demand) =
    d.action match {
      case a if a == Transparency || a.isChildOf(Transparency) => IO(Recommendation.grant(_, d.id))
      case Access                                              => getRecAccess(pr, d)
      case Delete                                              => getRecDelete(pr, d)
      case RevokeConsent                                       => getRecRevoke(pr, d)
      case Object | Restrict                                   => IO(Recommendation.grant(_, d.id))
      case _ => IO.raiseError(new NotImplementedError)
    }

  private def getRecAccess(pr: PrivacyRequest, d: Demand) =
    for {
      timeline    <- repos.events.getTimeline(pr.dataSubject.get)
      regulations <- repos.regulations.get(pr.appId)
      selectors   <- repos.privacyScope.getSelectors(pr.appId, active = true)
      eps = timeline.eligiblePrivacyScope(Some(pr.timestamp), regulations, selectors)

      psr   = d.getPSR.orEmpty.zoomIn(selectors)
      psRec =
        if psr.isEmpty then eps
        else eps intersection eps

      (from, to) = d.getDateRangeR.getOrElse((None, None))
      pRec       = d.getProvenanceR.map(_._1).filter(_ != ProvenanceTerms.All)
      tRec       = d.getProvenanceR.flatMap(_._2)
      // TODO: data reference restriction

      dcs = psRec.triples.map(_.dataCategory)
      r   = Recommendation(_, d.id, Some(Status.Granted), None, dcs, from, to, pRec, tRec)
    } yield r

  private def getRecDelete(pr: PrivacyRequest, d: Demand) =
    for {
      timeline    <- repos.events.getTimeline(pr.dataSubject.get)
      regulations <- repos.regulations.get(pr.appId)
      selectors   <- repos.privacyScope.getSelectors(pr.appId, active = true)
      events = timeline.compiledEvents(Some(pr.timestamp), regulations, selectors)
      eps    = Timeline.eligiblePrivacyScope(events)

      rdcs        = d.getPSR.orEmpty.zoomIn(selectors).dataCategories
      restDCs     =
        if rdcs.isEmpty then DataCategory.getMostGranular(DataCategory.All, selectors)
        else rdcs
      epsDCs      = eps.triples.map(_.dataCategory)
      filteredDCs = events.foldLeft(epsDCs intersect restDCs)(
        (acc, ev) =>
          ev match {
            case lb: TimelineEvent.LegalBase if lb.eType != LegalBaseTerms.LegitimateInterest =>
              acc diff lb.scope.dataCategories
            case lb: TimelineEvent.ConsentGiven => acc diff lb.scope.dataCategories
            case _                              => acc
          }
      )
      (from, to)  = d.getDateRangeR.getOrElse((None, None))
      pRec        = d.getProvenanceR.map(_._1).filter(_ != ProvenanceTerms.All)
      tRec        = d.getProvenanceR.flatMap(_._2)
      // TODO: data reference restriction
      r           =
        if filteredDCs.size == 0 then
          // format: off
          Recommendation(_, d.id, Some(Status.Denied), Some(Motive.ValidReasons), filteredDCs, from, to, pRec, tRec)
        else if epsDCs.size == filteredDCs.size then
          Recommendation(_, d.id, Some(Status.Granted), None, filteredDCs, from, to, pRec, tRec)
        else 
          Recommendation(_, d.id, Some(Status.PartiallyGranted), Some(Motive.ValidReasons), filteredDCs, from, to, pRec, tRec)
          // format: on
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

  private def complete(app: PCEApp, d: Demand) =
    val auto: Boolean =
      d.action match {
        case a if a == Transparency || a.isChildOf(Transparency) => app.autoResolve.transparency
        case Access                                              => app.autoResolve.access
        case Delete                                              => app.autoResolve.delete
        case RevokeConsent                                       => app.autoResolve.consents
        case Object | Restrict                                   => app.autoResolve.consents
        case _                                                   => false
      }

    if auto then
      CommandCreateResponse.create(d.id) map (c => List(c)) >>= repos.commands.addCreateResp
    else repos.demandsToReview.add(List(d.id))

}

object RequestRecommender {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new RequestRecommender(repos)

    def loop(): IO[Unit] =
      for {
        cs <- repos.commands.getCreateRec(10)
        _  <- cs.parTraverse_(
          c => {
            val dId = c.dId
            val p   = for {
              _ <- logger.info(s"Creating recommendation for demand $dId")
              _ <- reqProc.processDemand(c)
              _ <- logger.info(s"Recommendation for demand $dId created")
            } yield ()

            p.handleErrorWith(
              e =>
                logger
                  .error(e)(s"Error processing demand $dId - ${e.getMessage}")
                // .flatMap(_ => IO.sleep(5.second))
                // .flatMap(_ => repos.commands.addCreateRec(List(c)))
            )
          }
        )

        _ <- IO.sleep(5.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
