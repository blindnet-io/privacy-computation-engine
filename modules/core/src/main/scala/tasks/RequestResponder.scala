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
import io.blindnet.pce.services.external.StorageInterface

class RequestResponder(
    repos: Repositories,
    storage: StorageInterface
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val transparency = TransparencyDemands(repos)

  private def createResponse(dId: UUID): IO[Unit] =
    for {
      // TODO .get
      d       <- repos.privacyRequest.getDemand(dId).map(_.get)
      pr      <- repos.privacyRequest.getRequest(d).map(_.get)
      respOpt <- repos.privacyRequest.getDemandResponse(dId)
      resp    <- respOpt match {
        case None       =>
          // TODO: do we create a new UNDER-REVIEW response here?
          IO.raiseError(new NotFoundException(s"Demand response with id $dId not found"))
        case Some(resp) =>
          IO.pure(resp)
      }

      _ <- createResponse(pr, d, resp)
    } yield ()

  private def createResponse(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    // TODO: get setting about which action to respond and which needs review
    resp.status match {
      case UnderReview =>
        d.action match {
          case a if a == Transparency || a.isChildOf(Transparency) =>
            createResponseTransparency(pr, d, resp)

          case Access =>
            createResponseAccess(pr, d, resp)

          case _ => IO.raiseError(new NotImplementedError)
        }
      // ignore already processed request
      case _           => IO.unit
    }

  private def createResponseTransparency(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    for {
      answer    <- transparency.getAnswer(d, pr.appId, pr.dataSubject)
      id        <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp = PrivacyResponse(
        id,
        resp.responseId,
        resp.demandId,
        timestamp,
        resp.action,
        Status.Granted,
        answer = Some(answer)
      )

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()

  // TODO: .get
  private def createResponseAccess(pr: PrivacyRequest, d: Demand, resp: PrivacyResponse): IO[Unit] =
    for {
      rec <- repos.privacyRequest.getRecommendation(d.id).map(_.get)

      newRespId <- UUIDGen.randomUUID[IO]

      cbId <- UUIDGen.randomUUID[IO]
      _    <- repos.callbacks.set(cbId, pr.appId, newRespId)
      // TODO
      _ = println()
      _ = println(cbId)
      _ = println()
      // _    <- storage.requestAccessLink(appId, dId, cbId, ds, rec)
      _ <- storage.requestAccessLink(pr.appId, d.id, cbId, pr.dataSubject, rec).attempt

      timestamp <- Clock[IO].realTimeInstant
      newResp = PrivacyResponse(
        newRespId,
        resp.responseId,
        d.id,
        timestamp,
        d.action,
        Status.Granted
      )
      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()

  private def processAccess(pr: PrivacyRequest, d: Demand): IO[Unit] =
    for {
      recOpt <- repos.privacyRequest.getRecommendation(d.id)
      rec    <- recOpt match {
        case Some(r) => IO.pure(r)
        case None    =>
          for {
            id <- UUIDGen[IO].randomUUID
            ds = NonEmptyList.fromList(pr.dataSubject).get
            // TODO: take into account the date of the request
            timeline <- repos.privacyScope.getTimeline(pr.appId, ds)
            eps = timeline.eligiblePrivacyScope
            dcs = eps.triples.map(_.dataCategory)
            r   = Recommendation(id, d.id, dcs, None, None, None)
            _ <- repos.privacyRequest.storeRecommendation(r)
          } yield r
      }
    } yield ()

}

object RequestResponder {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories, storage: StorageInterface): IO[Unit] = {
    val reqProc = new RequestResponder(repos, storage)

    def loop(): IO[Unit] =
      for {
        ids <- repos.demandsToRespond.get()
        _   <- ids.parTraverse_(
          id => {
            val p = for {
              _ <- logger.info(s"Creating response for demand $id")
              _ <- repos.demandsToRespond.remove(NonEmptyList.one(id))
              _ <- reqProc.createResponse(id)
              _ <- logger.info(s"Response for demand $id created")
            } yield ()

            p.handleErrorWith(
              logger
                .error(_)(s"Error responding demand $id")
                .flatMap(_ => repos.demandsToRespond.store(List(id)))
            )
          }
        )

        _ <- IO.sleep(1.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
