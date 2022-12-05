package io.blindnet.pce
package requesthandlers.calculator

import java.util.UUID

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.*
import io.blindnet.pce.priv.DataSubject
import io.blindnet.pce.services.external.StorageInterface
import io.blindnet.pce.util.extension.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import priv.Recommendation
import priv.privacyrequest.*
import priv.terms.*
import db.repositories.Repositories
import io.blindnet.pce.db.repositories.CBData
import fs2.Stream

// TODO: refactor
class ResponseCalculator(
    repos: Repositories
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val transparency = TransparencyCalculator(repos)
  val general      = GeneralCalculator(repos)

  private def createResponse(ccr: CommandCreateResponse): IO[Unit] =
    for {
      responses <- repos.privacyRequest.getDemandResponses(ccr.dId)
      d         <- repos.privacyRequest.getDemand(ccr.dId, true).map(_.get)
      pr        <- repos.privacyRequest.getRequestFromDemand(d.id).map(_.get)
      rec       <- repos.privacyRequest.getRecommendation(d.id).map(_.get)
      app       <- repos.app.get(pr.appId).map(_.get)
      _         <- responses.traverse(r => processResponse(ccr, r, d, pr, rec, app))
    } yield ()

  private def processResponse(
      ccr: CommandCreateResponse,
      resp: PrivacyResponse,
      d: Demand,
      pr: PrivacyRequest,
      r: Recommendation,
      app: PCEApp
  ): IO[Unit] =
    resp.status match {
      case UnderReview =>
        for {

          newResp <- createResponse(pr, ccr, d, resp, r)
          // TODO: 3 atomic inserts, rollback if this IO fails
          _       <- repos.privacyRequest.storeNewResponse(newResp)
          _       <- storeEvent(pr, d).whenA(newResp.status == Granted)
          _       <-
            createStorageCommand(d.id, d.action, newResp.status, newResp.eventId)
              .whenA(app.dac.usingDac)
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

      case Portability =>
        general.createResponse(pr, ccr, d, resp, r)

      case Other =>
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

  private def createStorageCommand(
      dId: UUID,
      action: Action,
      status: Status,
      preId: ResponseEventId
  ) =
    (status, action) match {
      case (Status.Granted | Status.PartiallyGranted, Action.Access) =>
        for {
          c <- CommandInvokeStorage.createGet(dId, preId.value)
          _ <- repos.commands.pushInvokeStorage(List(c))
        } yield ()

      case (Status.Granted | Status.PartiallyGranted, Action.Delete) =>
        for {
          c <- CommandInvokeStorage.createDelete(dId, preId.value)
          _ <- repos.commands.pushInvokeStorage(List(c))
        } yield ()

      case _ => IO.unit
    }

}

object ResponseCalculator {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories): IO[Unit] = {
    val reqProc = new ResponseCalculator(repos)

    def process(c: CommandCreateResponse) =
      (for {
        _ <- logger.info(s"Creating response for demand ${c.dId}")
        _ <- reqProc.createResponse(c)
        _ <- logger.info(s"Response for demand ${c.dId} created")
      } yield ()).handleErrorWith(
        e =>
          logger
            .error(e)(s"Error creating response for demand ${c.dId}\n${e.getMessage}")
            .flatMap(_ => repos.commands.pushCreateResponse(List(c.addRetry)))
      )

    Stream
      .eval(repos.commands.popCreateResponse(5))
      .map(cs => Stream.emits(cs).evalMap(c => process(c)))
      .parJoin(10)
      .delayBy(5.second)
      .repeat
      .compile
      .drain
  }

}
