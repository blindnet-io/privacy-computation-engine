package io.blindnet.pce
package requesthandlers.calculator

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
import io.blindnet.pce.model.DemandToRespond
import io.blindnet.pce.priv.DataSubject

// TODO: refactor
class ResponseCalculator(
    repos: Repositories,
    storage: StorageInterface
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val transparency = TransparencyCalculator(repos)
  val general      = GeneralCalculator(repos, storage)

  private def createResponse(dtr: DemandToRespond): IO[Unit] =
    for {
      // TODO .get
      resp <- repos.privacyRequest.getDemandResponse(dtr.dId).map(_.get)
      _    <- resp.status match {
        case UnderReview =>
          // TODO: rollback if fails
          for {
            d       <- repos.privacyRequest.getDemand(dtr.dId, true).map(_.get)
            pr      <- repos.privacyRequest.getRequest(d).map(_.get)
            r       <- repos.privacyRequest.getRecommendation(d.id).map(_.get)
            newResp <- createResponse(pr, dtr, d, resp, r)
            _       <- repos.privacyRequest.storeNewResponse(newResp)
            _       <- if (newResp.status == Granted) then storeEvent(pr, d) else IO.unit
            _       <- callStorage(pr.appId, resp.id, d, pr.dataSubject, r)
            // _       <- callStorage(pr.appId, newResp.id, d, pr.dataSubject, r).attempt
          } yield ()
        case _           => logger.info(s"Demand ${dtr.dId} not UNDER-REVIEW")
      }
    } yield ()

  private def createResponse(
      pr: PrivacyRequest,
      dtr: DemandToRespond,
      d: Demand,
      resp: PrivacyResponse,
      r: Recommendation
  ): IO[PrivacyResponse] =
    d.action match {
      case a if a == Transparency || a.isChildOf(Transparency) =>
        transparency.createResponse(pr, d, resp.responseId, r)

      case Access =>
        general.createResponse(pr, dtr, d, resp, r)

      case Delete =>
        general.createResponse(pr, dtr, d, resp, r)

      case RevokeConsent =>
        general.createResponse(pr, dtr, d, resp, r)

      case _ => IO.raiseError(new NotImplementedError)
    }

  private def storeEvent(
      pr: PrivacyRequest,
      d: Demand
  ) =
    d.action match {
      case RevokeConsent =>
        for {
          cId <- IO(d.restrictions.head.asInstanceOf[Restriction.Consent].consentId)
          _   <- repos.events.addConsentRevoked(cId, pr.dataSubject.get, pr.timestamp)
        } yield ()
      case _             =>
        IO.unit
    }

  private def callStorage(
      appId: UUID,
      rId: UUID,
      d: Demand,
      ds: Option[DataSubject],
      r: Recommendation
  ) =
    (d.action, ds) match {
      case (Access, Some(ds)) =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, appId, rId)
          _    <- storage.requestAccess(cbId, appId, d.id, ds, r)
        } yield ()
      case (Delete, Some(ds)) =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, appId, rId)
          _    <- storage.requestDeletion(cbId, appId, d.id, ds, r)
        } yield ()
      case _                  => IO.unit
    }

}

object ResponseCalculator {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories, storage: StorageInterface): IO[Unit] = {
    val reqProc = new ResponseCalculator(repos, storage)

    def loop(): IO[Unit] =
      for {
        ds <- repos.demandsToRespond.get()
        _  <- ds.parTraverse_(
          d => {
            val id = d.dId
            val p  = for {
              _ <- logger.info(s"Creating response for demand $id")
              _ <- repos.demandsToRespond.remove(NonEmptyList.one(id))
              _ <- reqProc.createResponse(d)
              _ <- logger.info(s"Response for demand $id created")
            } yield ()

            p.handleErrorWith(
              e =>
                logger
                  .error(e)(s"Error creating response for demand $id - ${e.getMessage}")
                  .flatMap(_ => repos.demandsToRespond.add(List(d)))
            )
          }
        )

        _ <- IO.sleep(1.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
