package io.blindnet.privacy
package tasks

import cats.effect.*
import cats.implicits.*
import db.repositories.Repositories
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import io.blindnet.privacy.model.error.*
import io.blindnet.privacy.model.vocabulary.request.*
import io.blindnet.privacy.model.vocabulary.terms.*
import cats.effect.std.UUIDGen
import scala.concurrent.duration.*
import io.blindnet.privacy.util.extension.*

class RequestProcessor(
    repos: Repositories
) {

  import Action.*
  import Status.*

  val transparency = TransparencyDemands(repos)

  def processRequest(reqId: String): IO[Unit] = {
    for {
      req <- repos.privacyRequest
        .getRequest(reqId)
        .orNotFound(s"Request with id $reqId not found")

      _ <- req.demands.parTraverse_(d => processDemand(req, d))
    } yield ()
  }

  private def processDemand(request: PrivacyRequest, demand: Demand): IO[Unit] = {
    for {
      respOpt <- repos.privacyRequest.getDemandResponse(demand.id)
      resp    <- respOpt match {
        case None       =>
          // TODO: do we create a new UNDER-REVIEW response here?
          IO.raiseError(new NotFoundException(s"Demand response with id ${demand.id} not found"))
        case Some(resp) =>
          IO.pure(resp)
      }

      _ <- processAction(request, demand, resp)
    } yield ()

  }

  private def processAction(
      request: PrivacyRequest,
      demand: Demand,
      resp: PrivacyResponse
  ): IO[Unit] = {
    demand.action match {

      case t if resp.status == UnderReview && (t == Transparency || t.isChildOf(Transparency)) =>
        processTransparency(request, demand, resp)

      case t if resp.status == UnderReview && t == Access =>
        processAccess(request, demand)

      case _ => IO.raiseError(new NotImplementedError)

    }
  }

  private def processTransparency(
      request: PrivacyRequest,
      demand: Demand,
      resp: PrivacyResponse
  ): IO[Unit] = {
    for {
      answer    <- transparency.getAnswer(demand, request.appId, request.dataSubject)
      id        <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      newResp = resp.copy(
        id = id.toString,
        timestamp = timestamp,
        status = Status.Granted,
        answer = Some(answer)
      )

      _ <- repos.privacyRequest.storeNewResponse(newResp)
    } yield ()
  }

  private def processAccess(request: PrivacyRequest, demand: Demand): IO[Unit] =
    repos.pendingDemands.storePendingDemand(request.appId, demand.id)

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
