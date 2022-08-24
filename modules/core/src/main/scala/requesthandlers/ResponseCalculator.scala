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
import io.blindnet.pce.services.external.StorageInterface
import io.blindnet.pce.model.DemandToRespond

// TODO: refactor
class ResponseCalculator(
    repos: Repositories,
    storage: StorageInterface
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val transparency = TransparencyDemands(repos)

  private def createResponse(dtr: DemandToRespond): IO[Unit] =
    for {
      // TODO .get
      d       <- repos.privacyRequest.getDemand(dtr.dId, true).map(_.get)
      pr      <- repos.privacyRequest.getRequest(d).map(_.get)
      respOpt <- repos.privacyRequest.getDemandResponse(dtr.dId)
      resp    <- respOpt match {
        case None       =>
          // TODO: do we create a new UNDER-REVIEW response here?
          IO.raiseError(new NotFoundException(s"Demand response with id ${dtr.dId} not found"))
        case Some(resp) =>
          IO.pure(resp)
      }

      _ <- createResponse(pr, dtr, d, resp)
    } yield ()

  private def createResponse(
      pr: PrivacyRequest,
      dtr: DemandToRespond,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    resp.status match {
      case UnderReview =>
        d.action match {
          case a if a == Transparency || a.isChildOf(Transparency) =>
            createResponseTransparency(pr, d, resp)

          case Access =>
            createResponseAccess(pr, dtr, d, resp)

          case Delete =>
            createResponseDelete(pr, dtr, d, resp)

          case _ => IO.raiseError(new NotImplementedError)
        }
      // ignore already processed request
      case _           => IO.unit
    }

  // TODO: .get
  private def createResponseTransparency(
      pr: PrivacyRequest,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    for {
      r <- repos.privacyRequest.getRecommendation(d.id).map(_.get)

      id        <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp <-
        r.status match {
          case Some(Status.Granted) | None =>
            for {
              answer <- transparency.getAnswer(d, pr.appId, pr.dataSubject)
              // format: off
              newResp = PrivacyResponse(id, resp.responseId, d.id, timestamp, d.action, Status.Granted, answer = Some(answer))
            } yield newResp
            
          case Some(s) =>
            IO.pure(PrivacyResponse(id, resp.responseId, d.id, timestamp, d.action, s, r.motive))
        }

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()

  // TODO: .get
  private def createResponseAccess(
      pr: PrivacyRequest,
      dtr: DemandToRespond,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    for {
      r <- repos.privacyRequest.getRecommendation(d.id).map(_.get)

      newRespId <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp <-
        r.status match {
          case Some(Status.Granted) =>
            for {
              cbId <- UUIDGen.randomUUID[IO]
              _    <- repos.callbacks.set(cbId, pr.appId, newRespId)
              // TODO
              // _    <- storage.requestAccessLink(cbId, pr.appId, d.id, pr.dataSubject.get, r).attempt
              _    <- storage.requestAccessLink(cbId, pr.appId, d.id, pr.dataSubject.get, r)

              msg  = dtr.data.hcursor.downField("msg").as[String].toOption
              lang = dtr.data.hcursor.downField("lang").as[String].toOption

              newResp = PrivacyResponse(
                newRespId,
                resp.responseId,
                d.id,
                timestamp,
                d.action,
                Status.Granted,
                message = msg,
                lang = lang
              )
            } yield newResp

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, s, r.motive))

          case None =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, Status.Denied, r.motive))
        }

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()

  private def createResponseDelete(
      pr: PrivacyRequest,
      dtr: DemandToRespond,
      d: Demand,
      resp: PrivacyResponse
  ): IO[Unit] =
    for {
      r <- repos.privacyRequest.getRecommendation(d.id).map(_.get)

      newRespId <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp <-
        r.status match {
          case Some(Status.Granted) =>
            for {
              cbId <- UUIDGen.randomUUID[IO]
              _    <- repos.callbacks.set(cbId, pr.appId, newRespId)
              // TODO
              // _    <- storage.requestDeletion(cbId, pr.appId, d.id, pr.dataSubject.get, r).attempt
              _    <- storage.requestDeletion(cbId, pr.appId, d.id, pr.dataSubject.get, r)

              msg  = dtr.data.hcursor.downField("msg").as[String].toOption
              lang = dtr.data.hcursor.downField("lang").as[String].toOption

              newResp = PrivacyResponse(
                newRespId,
                resp.responseId,
                d.id,
                timestamp,
                d.action,
                Status.Granted,
                message = msg,
                lang = lang
              )
            } yield newResp

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, s, r.motive))

          case None =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, Status.Denied, r.motive))
        }

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()

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
