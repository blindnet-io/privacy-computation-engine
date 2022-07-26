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
import api.endpoints.payload.*
import db.repositories.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*
import services.requests.TransparencyDemands
import java.time.Instant

class PrivacyRequestService(
    repositories: Repositories
) {

  val transparency = new TransparencyDemands(repositories)

  private def createPrivacyRequest(req: PrivacyRequestPayload, appId: String) = {
    for {
      // TODO: reject if number of demands is large

      reqId     <- UUIDGen.randomUUID[IO]
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      date      <- Clock[IO].realTimeInstant

      _ <-
        if req.demands.map(_.id).toSet.size == req.demands.size then IO.unit
        else IO.raiseError(BadRequestException("Demands have duplicate ids"))

      demands = req.demands.zip(demandIds).map {
        case (d, id) =>
          Demand(
            d.id,
            id.toString(),
            d.action,
            d.message,
            d.language,
            d.data.getOrElse(List.empty),
            // TODO: restrictions
            List.empty,
            d.target.getOrElse(Target.System)
          )
      }

    } yield PrivacyRequest(
      reqId.toString(),
      appId,
      date,
      req.dataSubject,
      demands
    )
  }

  def processRequest(
      req: PrivacyRequestPayload,
      appId: String
  ): IO[PrivacyRequestResponsePayload] = {
    for {
      pr   <- createPrivacyRequest(req, appId)
      id   <- UUIDGen[IO].randomUUID
      date <- Clock[IO].realTimeInstant

      (invalid, valid) = PrivacyRequest.validateDemands(pr)

      // TODO: store request
      results <- valid.parTraverse(d => processDemand(d, pr.appId, pr.dataSubjectIds))

      invalidResults <- invalid.traverse(
        d => createInvalidDemandResponse(d._2, d._1.mkString_("\n"))
      )

    } yield PrivacyRequestResponsePayload(id.toString, pr.id, date, results ++ invalidResults)
  }

  private def processDemand(
      demand: Demand,
      appId: String,
      userIds: List[DataSubject]
  ): IO[DemandResponse] = {

    for {
      date <- Clock[IO].realTimeInstant
      res  <- demand.action match {
        case t if t.isChildOf(Action.Transparency) =>
          transparency.processTransparencyDemand(demand, appId, userIds, date)
        case _                                     => IO.raiseError(new NotImplementedError)
      }

    } yield res

  }

  private def createInvalidDemandResponse(
      demand: Demand,
      message: String
  ): IO[DemandResponse] = {
    for {
      date <- Clock[IO].realTimeInstant
    } yield DemandResponse(
      demand.refId,
      demand.id,
      date,
      demand.action,
      Status.Denied,
      Json.Null,
      Some(message),
      lang = "en",
      None,
      None
    )
  }

}
