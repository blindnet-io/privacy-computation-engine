package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.pce.api.endpoints.messages.consumerinterface.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.services.util.*
import io.blindnet.pce.util.extension.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.terms.*
import io.blindnet.pce.services.storage.StorageInterface

class DataConsumerInterfaceService(
    repos: Repositories,
    storage: StorageInterface
) {

  def getPendingDemands(appId: UUID) = {
    for {
      dIds    <- repos.pendingDemands.getPendingDemandIds(appId)
      demands <- NonEmptyList.fromList(dIds) match {
        case None      => IO(List.empty[Demand])
        case Some(ids) => repos.privacyRequest.getDemands(ids)
      }
      reqs    <- NonEmptyList.fromList(demands.map(_.reqId)) match {
        case None      => IO(List.empty[PrivacyRequest])
        case Some(ids) => repos.privacyRequest.getRequestsSimple(ids)
      }

      res = (demands.sortBy(_.reqId) zip reqs.sortBy(_.id)).map(PendingDemandPayload.fromPrivDemand)
    } yield res
  }

  def getPendingDemandDetails(appId: UUID, dId: UUID) =
    for {
      _ <- repos.privacyRequest.demandExist(appId, dId).emptyNotFound(s"Demand $dId not found")
      d <- repos.privacyRequest.getDemand(dId).orFail(s"Demand $dId not found")
      rId = d.reqId
      req <- repos.privacyRequest.getRequestSimple(rId).orFail(s"Request $rId not found")
      rec <- repos.privacyRequest.getRecommendation(dId)
      res = PendingDemandDetailsPayload.fromPrivDemand(d, req, rec)
    } yield res

  def approveDemand(appId: UUID, req: ApproveDemandPayload) =
    for {
      dId <- IO.pure(req.id)
      _   <- repos.privacyRequest.demandExist(appId, dId).emptyNotFound(s"Demand $dId not found")
      ds  <- repos.privacyRequest.getDataSubject(dId)
      r   <- repos.privacyRequest.getDemandResponse(dId).orFail(s"Response for $dId not found")
      _   <-
        if r.status == Status.Granted
        then BadRequestException(s"Demand $dId already granted".asJson).raise
        else IO.unit
      rec <- repos.privacyRequest.getRecommendation(dId).orNotFound(s"Rec for $dId not found")

      newRespId <- UUIDGen.randomUUID[IO]

      cbId <- UUIDGen.randomUUID[IO]
      _    <- repos.callbacks.set(cbId, appId, newRespId)
      // TODO
      _ = println()
      _ = println(cbId)
      _ = println()
      // _    <- storage.requestAccessLink(appId, dId, cbId, ds, rec)
      _ <- storage.requestAccessLink(appId, dId, cbId, ds, rec).attempt

      timestamp <- Clock[IO].realTimeInstant
      newResp = PrivacyResponse(
        newRespId,
        r.responseId,
        r.demandId,
        timestamp,
        r.action,
        Status.Granted
      )
      _ <- repos.privacyRequest.storeNewResponse(newResp)

      _ <- repos.pendingDemands.removePendingDemand(appId, dId)
    } yield ()

}
