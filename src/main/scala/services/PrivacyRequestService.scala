package io.blindnet.privacy
package services

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*
// import services.requests.TransparencyDemands
import io.blindnet.privacy.model.error.given
import java.time.Instant

class PrivacyRequestService(
    repositories: Repositories
) {

  // val transparency = new TransparencyDemands(repositories)

  private def failBadRequest(msg: String) =
    BadRequestException(BadPrivacyRequestPayload(msg).asJson).raise

  private def failNotFound(msg: String) =
    NotFoundException(msg).raise

  private def validateRequest(req: PrivacyRequest) = {
    for {
      _ <-
        if req.demands.map(_.action).toSet.size == req.demands.size then IO.unit
        else failBadRequest("2 or more demands have duplicate action types")

      (invalid, _) = PrivacyRequest.validateDemands(req)

      _ <-
        if invalid.length == 0 then IO.unit
        else failBadRequest(invalid.foldLeft("")((acc, cur) => acc + cur._1 + "\n"))

      _ <- NonEmptyList
        .fromList(req.dataSubject)
        .fold(IO.unit)(
          repositories.dataSubject
            .known(req.appId, _)
            .flatMap(if _ then IO.unit else failBadRequest("Unknown data subject"))
        )

    } yield ()

  }

  def createPrivacyRequest(req: CreatePrivacyRequestPayload, appId: String) = {
    for {
      reqId     <- UUIDGen.randomUUID[IO]
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      timestamp <- Clock[IO].realTimeInstant

      demands = req.demands.zip(demandIds).map {
        case (d, id) => PrivacyRequestDemand.toPrivDemand(id.toString(), d)
      }

      pr = PrivacyRequest(
        reqId.toString(),
        appId,
        timestamp,
        req.target.getOrElse(Target.System),
        req.email,
        req.dataSubject,
        demands
      )

      _ <- validateRequest(pr)

      _ <- repositories.privacyRequest.store(pr)

    } yield PrivacyRequestCreatedPayload(reqId.toString)
  }

  def getResponse(requestId: String, appId: String) = {

    for {
      exist <- repositories.privacyRequest.requestExist(requestId)

      _ <-
        if exist then IO.unit
        else failNotFound("Request not found")

      privResponsesOpt <- repositories.privacyRequest.getResponse(requestId)

      privResponses <- privResponsesOpt match {
        case None      => IO.raiseError(InternalException())
        case Some(prs) => IO.pure(prs)
      }

      resp = privResponses.map(PrivacyResponsePayload.fromPrivPrivacyResponse)
    } yield resp
  }

  // def processRequest(
  //     req: CreatePrivacyRequestPayload,
  //     appId: String
  // ): IO[CreatePrivacyRequestResponsePayload] = {
  //   for {
  //     pr   <- createPrivacyRequest(req, appId)
  //     id   <- UUIDGen[IO].randomUUID
  //     date <- Clock[IO].realTimeInstant

  //     (invalid, valid) = PrivacyRequest.validateDemands(pr)

  //     // TODO: store request
  //     results <- valid.parTraverse(d => processDemand(d, pr.appId, pr.dataSubject))

  //     invalidResults <- invalid.traverse(
  //       d => createInvalidDemandResponse(d._2, d._1.mkString_("\n"))
  //     )

  //   } yield PrivacyRequestResponsePayload(id.toString, pr.id, date, results ++ invalidResults)
  // }

  // private def processDemand(
  //     demand: Demand,
  //     appId: String,
  //     userIds: List[DataSubject]
  // ): IO[DemandResponse] = {

  //   for {
  //     date <- Clock[IO].realTimeInstant
  //     res  <- demand.action match {
  //       case t if t == Action.Transparency || t.isChildOf(Action.Transparency) =>
  //         transparency.processTransparencyDemand(demand, appId, userIds, date)
  //       case _ => IO.raiseError(new NotImplementedError)
  //     }

  //   } yield res

  // }

}
