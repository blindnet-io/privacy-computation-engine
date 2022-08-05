package io.blindnet.privacy
package tasks

import cats.effect.*
import cats.implicits.*
import db.repositories.Repositories
import state.State
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import io.blindnet.privacy.model.error.*
import io.blindnet.privacy.model.vocabulary.request.*
import io.blindnet.privacy.model.vocabulary.terms.*
import cats.effect.std.UUIDGen

class RequestProcessor(
    repos: Repositories
) {

  val transparency = TransparencyDemands(repos)

  def processRequest(reqId: String): IO[Unit] = {
    for {
      reqOpt <- repos.privacyRequest.getRequest(reqId)
      req    <- reqOpt match {
        case None      => IO.raiseError(new NotFoundException(s"Request with id $reqId not found"))
        case Some(req) => IO.pure(req)
      }

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

      _ <-
        if resp.status == Status.Granted then IO.unit
        else processAction(request, demand, resp)

    } yield ()

  }

  private def processAction(
      request: PrivacyRequest,
      demand: Demand,
      resp: PrivacyResponse
  ): IO[Unit] = {
    demand.action match {

      case t if t == Action.Transparency || t.isChildOf(Action.Transparency) =>
        processTransparency(request, demand, resp)

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
      // TODO: store response
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

}

object RequestProcessor {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories, state: State): IO[Unit] = {
    val reqProc = new RequestProcessor(repos)

    def loop(): IO[Unit] =
      for {
        reqId <- state.pendingRequests.take
        _     <- logger.info(s"Processing new request $reqId")
        // TODO: handle errors - repeat

        fib <- reqProc
          .processRequest(reqId)
          .flatMap(_ => logger.info(s"Request $reqId processed"))
          .handleErrorWith(e => logger.error(e)(s"Error processing request with id $reqId"))
          .start

        _ <- loop()
      } yield ()

    loop()
  }

}
