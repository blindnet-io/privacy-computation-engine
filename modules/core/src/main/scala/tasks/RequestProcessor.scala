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

class RequestProcessor(
    repos: Repositories
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val transparency = TransparencyDemands(repos)

  def processRequest(reqId: UUID): IO[Unit] = {
    for {
      req <- repos.privacyRequest
        .getRequest(reqId)
        .orNotFound(s"Request with id $reqId not found")

      _ <- req.demands.parTraverse_(d => processDemand(req, d))
    } yield ()
  }

  private def processDemand(pr: PrivacyRequest, d: Demand): IO[Unit] = {
    for {
      respOpt <- repos.privacyRequest.getDemandResponse(d.id)
      resp    <- respOpt match {
        case None       =>
          // TODO: do we create a new UNDER-REVIEW response here?
          IO.raiseError(new NotFoundException(s"Demand response with id ${d.id} not found"))
        case Some(resp) =>
          IO.pure(resp)
      }

      _ <- processAction(pr, d, resp)
    } yield ()

  }

  private def processAction(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] = {
    d.action match {

      case t if resp.status == UnderReview && (t == Transparency || t.isChildOf(Transparency)) =>
        processTransparency(pr, d, resp)

      case t if resp.status == UnderReview && t == Access =>
        processAccess(pr, d)

      case _ => IO.raiseError(new NotImplementedError)

    }
  }

  private def processTransparency(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] = {
    for {
      answer    <- transparency.getAnswer(d, pr.appId, pr.dataSubject)
      id        <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp = resp.copy(
        id = id,
        timestamp = timestamp,
        status = Status.Granted,
        answer = Some(answer)
      )

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()
  }

  private def processAccess(pr: PrivacyRequest, d: Demand): IO[Unit] =
    for {
      recOpt <- repos.privacyRequest.getRecommendation(d.id)

      rec <- recOpt match {
        case Some(r) => IO.pure(r)
        case None    =>
          for {
            id <- UUIDGen[IO].randomUUID
            ds = NonEmptyList.fromList(pr.dataSubject).get
            timeline <- repos.privacyScope.getTimeline(pr.appId, ds)
            eps = timeline.eligiblePrivacyScope
            dcs = eps.triples.map(_.dataCategory)
            r   = Recommendation(id, d.id, dcs, None, None, None)
            _ <- repos.privacyRequest.storeRecommendation(r)
          } yield r
      }

      _ <- repos.pendingDemands.storePendingDemand(pr.appId, d.id)
    } yield ()

}

object RequestProcessor {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new RequestProcessor(repos)

    def loop(): IO[Unit] =
      for {
        reqId <- repos.pendingRequests.get()
        _     <- reqId match {
          case None        => IO.unit
          case Some(reqId) =>
            for {
              _   <- logger.info(s"Processing new request $reqId")
              fib <- reqProc
                .processRequest(reqId)
                .flatMap(_ => logger.info(s"Request $reqId processed"))
                .handleErrorWith(
                  e =>
                    logger
                      .error(e)(s"Error processing request with id $reqId")
                      .flatMap(_ => repos.pendingRequests.add(reqId))
                )
                .start
            } yield ()
        }

        // TODO: what do we do to not overwhelm the service with bunch of parallel requests?
        _     <- IO.sleep(1.seconds)
        _     <- loop()
      } yield ()

    loop()
  }

}
