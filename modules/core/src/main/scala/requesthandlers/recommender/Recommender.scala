package io.blindnet.pce
package requesthandlers.recommender

import java.util.UUID

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.api.endpoints.messages.privacyrequest.DateRangeRestriction
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.*
import io.blindnet.pce.priv.*
import io.blindnet.pce.util.extension.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import priv.Recommendation
import priv.privacyrequest.*
import priv.terms.*
import db.repositories.Repositories
import fs2.Stream
import doobie.enumerated.JdbcType.Timestamp
import java.time.Instant

object Recommendations {
  def createAccess(d: Demand, ctx: PSContext)(id: UUID) = {
    val scopeRestriction = d.getPSR.orEmpty
    val dataCategories   =
      if scopeRestriction.isEmpty then DataCategory.granularize(DataCategory.All, ctx.selectors)
      else
        scopeRestriction.triples
          .map(_.dataCategory)
          .flatMap(DataCategory.granularize(_, ctx.selectors))

    val (from, to) = d.getDateRangeR.getOrElse((None, None))
    val pRec       = d.getProvenanceR.map(_._1).filter(_ != ProvenanceTerms.All)
    val tRec       = d.getProvenanceR.flatMap(_._2)
    // TODO: data reference restriction

    Recommendation(id, d.id, Some(Status.Granted), None, dataCategories, from, to, pRec, tRec)
  }

  def createDelete(
      d: Demand,
      t: Instant,
      ctx: PSContext,
      timeline: Timeline,
      regulations: List[Regulation]
  ) = {
    val events = timeline.compiledEvents(Some(t), regulations)
    val eps    = Timeline.eligiblePrivacyScope(events)

    val rdcs           = d.getPSR.orEmpty.zoomIn(ctx).dataCategories
    val restrictionDCs =
      if rdcs.isEmpty then DataCategory.granularize(DataCategory.All, ctx.selectors)
      else rdcs

    val filteredDCs = events.foldLeft(restrictionDCs)(
      (acc, ev) =>
        ev match {
          case lb: TimelineEvent.LegalBase if lb.eType != LegalBaseTerms.LegitimateInterest =>
            acc diff lb.scope.dataCategories
          case lb: TimelineEvent.ConsentGiven => acc diff lb.scope.dataCategories
          case _                              => acc
        }
    )

    val (from, to) = d.getDateRangeR.getOrElse((None, None))
    val pRec       = d.getProvenanceR.map(_._1).filter(_ != ProvenanceTerms.All)
    val tRec       = d.getProvenanceR.flatMap(_._2)
    // TODO: data reference restriction

    // format: off
    if filteredDCs.size == 0 then
      Recommendation(_, d.id, Some(Status.Denied), Some(Motive.ValidReasons), filteredDCs, from, to, pRec, tRec)
    else if rdcs.size == 0 || rdcs.size == filteredDCs.size then
      Recommendation(_, d.id, Some(Status.Granted), None, filteredDCs, from, to, pRec, tRec)
    else 
      Recommendation(_, d.id, Some(Status.PartiallyGranted), Some(Motive.ValidReasons), filteredDCs, from, to, pRec, tRec)
    // format: on
  }

}

class Recommender(
    repos: Repositories
) {
  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def processDemand(c: CommandCreateRecommendation): IO[Unit] =
    for {
      d         <- repos.privacyRequest.getDemand(c.dId, true).map(_.get)
      pr        <- repos.privacyRequest.getRequestFromDemand(d.id).map(_.get)
      app       <- repos.app.get(pr.appId).map(_.get)
      responses <- repos.privacyRequest.getDemandResponses(c.dId)
      _         <- responses.traverse(r => processResponse(app, pr, d, c, r))
      _         <- complete(app, d)
    } yield ()

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
          // create deny recommendation
          for {
            _ <- storeRecommendation(rf)
            c <- CommandCreateResponse.create(d.id)
            _ <- repos.commands.pushCreateResponse(List(c))
          } yield (),
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

      case Access =>
        for {
          ctx <- repos.privacyScope.getContext(pr.appId)
          res = Recommendations.createAccess(d, ctx)
        } yield res

      case Delete =>
        for {
          ctx         <- repos.privacyScope.getContext(pr.appId)
          timeline    <- repos.events.getTimeline(pr.dataSubject.get, ctx)
          regulations <- repos.regulations.get(pr.appId, ctx)
          res = Recommendations.createDelete(d, pr.timestamp, ctx, timeline, regulations)
        } yield res

      case RevokeConsent     => getRecRevoke(pr, d)
      case Object | Restrict => IO(Recommendation.grant(_, d.id))
      case Portability       => IO(Recommendation.grant(_, d.id))
      case Other             => IO(Recommendation.grant(_, d.id))
      case _                 => IO.raiseError(new NotImplementedError)
    }

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
        case a if a == Transparency || a.isChildOf(Transparency) =>
          app.resolutionStrategy.isAutoTransparency
        case Access        => app.resolutionStrategy.isAutoAccess
        case Delete        => app.resolutionStrategy.isAutoDelete
        case RevokeConsent => app.resolutionStrategy.isAutoRevokeConsent
        case Object        => app.resolutionStrategy.isAutoObject
        case Restrict      => app.resolutionStrategy.isAutoRestrict
        case _             => false
      }

    if auto then
      CommandCreateResponse.create(d.id) map (c => List(c)) >>= repos.commands.pushCreateResponse
    else repos.demandsToReview.add(List(d.id))

}

object Recommender {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new Recommender(repos)

    def process(c: CommandCreateRecommendation) =
      (for {
        _ <- logger.info(s"Creating recommendation for demand ${c.dId}")
        _ <- reqProc.processDemand(c)
        _ <- logger.info(s"Recommendation for demand ${c.dId} created")
      } yield ()).handleErrorWith(
        e =>
          logger
            .error(e)(s"Error creating recommendation for demand ${c.dId}\n${e.getMessage}")
            .flatMap(_ => repos.commands.pushCreateRecommendation(List(c.addRetry)))
      )

    Stream
      .eval(repos.commands.popCreateRecommendation(10))
      .map(cs => Stream.emits(cs).evalMap(c => process(c)))
      .parJoin(10)
      .delayBy(5.second)
      .repeat
      .compile
      .drain
  }

}
