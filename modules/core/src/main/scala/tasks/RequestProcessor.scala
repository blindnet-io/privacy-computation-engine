package io.blindnet.pce
package tasks

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

      _ <- processDemand(pr, d, resp)
    } yield ()

  private def processDemand(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    // TODO: get setting about which action to respond and which needs review
    resp.status match {
      case UnderReview =>
        d.action match {
          case a if a == Transparency || a.isChildOf(Transparency) =>
            // processTransparency(pr, d, resp)
            // TODO
            if true
            then repos.demandsToRespond.add(List(d.id))
            else repos.demandsToReview.add(List(d.id))

          case Access =>
            for {
              _ <- createRecommendation(pr, d)
              _ <-
                // TODO
                if true
                then repos.demandsToRespond.add(List(d.id))
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
            ds = NonEmptyList.fromList(pr.dataSubject).get
            timeline <- repos.privacyScope.getTimeline(pr.appId, ds)
            eps = timeline.eligiblePrivacyScope(Some(pr.timestamp))

            psRec = d.getPSR.map(ps => eps intersection ps).getOrElse(eps)
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
