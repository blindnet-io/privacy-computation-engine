package io.blindnet.pce
package requesthandlers

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
import io.blindnet.pce.api.endpoints.messages.privacyrequest.DateRangeRestriction.apply
import io.blindnet.pce.model.DemandToRespond
import io.blindnet.pce.model.PCEApp
import io.blindnet.pce.priv.PrivacyScope
import io.blindnet.pce.priv.PrivacyScopeTriple

class RequestProcessor(
    repos: Repositories
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  private def processDemand(dId: UUID): IO[Unit] =
    for {
      // TODO .get
      d       <- repos.privacyRequest.getDemand(dId, true).map(_.get)
      pr      <- repos.privacyRequest.getRequest(d).map(_.get)
      respOpt <- repos.privacyRequest.getDemandResponse(dId)
      resp    <- respOpt match {
        case None       =>
          // TODO: do we create a new UNDER-REVIEW response here?
          IO.raiseError(new NotFoundException(s"Demand response with id $dId not found"))
        case Some(resp) =>
          IO.pure(resp)
      }

      app <- repos.app.get(pr.appId).map(_.get)
      _   <- processDemand(app, pr, d, resp)
    } yield ()

  private def processDemand(
      app: PCEApp,
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    resp.status match {
      case UnderReview =>
        d.action match {
          case a if a == Transparency || a.isChildOf(Transparency) =>
            if app.autoResolve.transparency
            then repos.demandsToRespond.add(List(DemandToRespond(d.id)))
            else repos.demandsToReview.add(List(d.id))

          case Access =>
            for {
              _ <- createRecommendation(pr, d)
              _ <-
                if app.autoResolve.access
                then repos.demandsToRespond.add(List(DemandToRespond(d.id)))
                else repos.demandsToReview.add(List(d.id))
            } yield ()

          case _ => IO.raiseError(new NotImplementedError)
        }
      // ignore already processed request
      case _           => IO.unit
    }

  private def createRecommendation(pr: PrivacyRequest, d: Demand): IO[Unit] =
    for {
      recOpt <- repos.privacyRequest.getRecommendation(d.id)
      rec    <- recOpt match {
        case Some(r) => IO.pure(r)
        case None    => {
          for {
            id <- UUIDGen[IO].randomUUID
            ds = pr.dataSubject.get
            timeline <- repos.events.getTimeline(pr.appId, ds)
            eps = timeline.eligiblePrivacyScope(Some(pr.timestamp))

            psr = d.getPSR.getOrElse(PrivacyScope.empty)
            psRec <-
              if psr.isEmpty then IO(eps)
              else
                for {
                  selectors <- repos.privacyScope.getSelectors(pr.appId, active = true)
                  triples = psr.triples.flatMap(
                    triple =>
                      for {
                        dc <- DataCategory.getSubTerms(triple.dataCategory, selectors)
                        pc <- ProcessingCategory.getSubTerms(triple.processingCategory)
                        pp <- Purpose.getSubTerms(triple.purpose)
                      } yield PrivacyScopeTriple(dc, pc, pp)
                  )
                  scope   = PrivacyScope(triples) intersection eps
                } yield scope

            drRec = d.getDateRangeR.getOrElse((None, None))
            pRec  = d.getProvenanceR.map(_._1)
            // TODO: data reference restriction

            dcs = psRec.triples.map(_.dataCategory)

            r = Recommendation(id, d.id, dcs, drRec._1, drRec._2, pRec)
            _ <- repos.privacyRequest.storeRecommendation(r)
          } yield r
        }
      }
    } yield ()

}

object RequestProcessor {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new RequestProcessor(repos)

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
