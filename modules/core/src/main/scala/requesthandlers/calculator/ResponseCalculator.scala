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
import io.blindnet.pce.priv.DataSubject
import io.blindnet.pce.model.*

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

  private def createResponse(ccr: CommandCreateResponse): IO[Unit] =
    for {
      responses <- repos.privacyRequest.getDemandResponses(ccr.dId)
      _         <- responses.traverse(r => processResponse(ccr, r))
    } yield ()

  private def processResponse(ccr: CommandCreateResponse, resp: PrivacyResponse): IO[Unit] =
    resp.status match {
      case UnderReview =>
        // TODO: rollback if fails
        for {
          d       <- repos.privacyRequest.getDemand(ccr.dId, true).map(_.get)
          pr      <- repos.privacyRequest.getRequest(d).map(_.get)
          r       <- repos.privacyRequest.getRecommendation(d.id).map(_.get)
          newResp <- createResponse(pr, ccr, d, resp, r)
          _       <- repos.privacyRequest.storeNewResponse(newResp)
          _       <- if (newResp.status == Granted) then storeEvent(pr, d) else IO.unit
          _       <- callStorage(pr.appId, newResp.eventId, d, pr.dataSubject, r)
          // _       <- callStorage(pr.appId, newResp.eventId, d, pr.dataSubject, r).attempt
        } yield ()
      case _           => logger.info(s"Response ${resp.id} not UNDER-REVIEW")
    }

  private def createResponse(
      pr: PrivacyRequest,
      ccr: CommandCreateResponse,
      d: Demand,
      resp: PrivacyResponse,
      r: Recommendation
  ): IO[PrivacyResponse] =
    resp.action match {
      case a if a == Transparency || a.isChildOf(Transparency) =>
        transparency.createResponse(resp, pr.appId, pr.timestamp, pr.dataSubject, d.restrictions, r)

      case Access =>
        general.createResponse(pr, ccr, d, resp, r)

      case Delete =>
        general.createResponse(pr, ccr, d, resp, r)

      case RevokeConsent =>
        general.createResponse(pr, ccr, d, resp, r)

      case Object =>
        general.createResponse(pr, ccr, d, resp, r)

      case Restrict =>
        general.createResponse(pr, ccr, d, resp, r)

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

      case Object => repos.events.addObject(d.id, pr.dataSubject.get, pr.timestamp)

      case Restrict => repos.events.addRestrict(d.id, pr.dataSubject.get, pr.timestamp)

      case _ => IO.unit
    }

  private def callStorage(
      appId: UUID,
      rEventId: ResponseEventId,
      d: Demand,
      ds: Option[DataSubject],
      r: Recommendation
  ) =
    (d.action, ds) match {
      case (Access, Some(ds)) =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, appId, rEventId)
          _    <- storage.requestAccess(cbId, appId, d.id, ds, r)
        } yield ()
      case (Delete, Some(ds)) =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, appId, rEventId)
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
        cs <- repos.commands.getCreateResp(5)
        _  <- cs.parTraverse_(
          c => {
            val dId = c.dId
            val p   = for {
              _ <- logger.info(s"Creating response for demand $dId")
              _ <- reqProc.createResponse(c)
              _ <- logger.info(s"Response for demand $dId created")
            } yield ()

            p.handleErrorWith(
              e =>
                logger
                  .error(e)(s"Error creating response for demand $dId - ${e.getMessage}")
                // .flatMap(_ => IO.sleep(5.second))
                // .flatMap(_ => repos.commands.addCreateResp(List(c)))
            )
          }
        )

        _ <- IO.sleep(5.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
