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
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.priv.Recommendation
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

class DataConsumerInterfaceService(
    repos: Repositories
) {

  def getPendingDemands(appId: UUID) = {
    for {
      dIds    <- repos.demandsToReview.get(appId)
      demands <- NonEmptyList.fromList(dIds) match {
        case None      => IO(List.empty[Demand])
        case Some(ids) => repos.privacyRequest.getDemands(ids)
      }
      reqs    <- NonEmptyList.fromList(demands.map(_.reqId)) match {
        case None      => IO(List.empty[PrivacyRequest])
        case Some(ids) => repos.privacyRequest.getRequests(ids)
      }

      res = demands.flatMap(
        d => reqs.find(_.id == d.reqId).map(PendingDemandPayload.fromPrivDemand(d, _))
      )
    } yield res
  }

  def getPendingDemandDetails(appId: UUID, dId: UUID) =
    for {
      _ <- repos.privacyRequest.demandExist(appId, dId).onFalseNotFound(s"Demand $dId not found")
      d <- repos.privacyRequest.getDemand(dId, false).orFail(s"Demand $dId not found")
      rId = d.reqId
      req <- repos.privacyRequest.getRequest(rId, false).orFail(s"Request $rId not found")
      rec <- repos.privacyRequest.getRecommendation(dId)
      res = PendingDemandDetailsPayload.fromPrivDemand(d, req, rec)
    } yield res

  def approveDemand(appId: UUID, req: ApproveDemandPayload) =
    for {
      _ <- repos.demandsToReview.remove(NonEmptyList.of(req.id))
      d <- CommandCreateResponse.create(
        req.id,
        // TODO: refactor
        Json.obj(
          "msg"  -> req.msg.map(_.asJson).getOrElse(Json.Null),
          "lang" -> req.lang.map(_.asJson).getOrElse(Json.Null)
        )
      )
      _ <- repos.commands.addCreateResp(List(d))
    } yield ()

  def denyDemand(appId: UUID, req: DenyDemandPayload) =
    for {
      r <- repos.privacyRequest.getRecommendation(req.id).orFail(s"No recommendation found")
      newR = r.copy(status = Some(Status.Denied), motive = Some(req.motive))
      _ <- repos.privacyRequest.updateRecommendation(newR)
      d <- CommandCreateResponse.create(
        req.id,
        // TODO: refactor
        Json.obj(
          "msg"  -> req.msg.map(_.asJson).getOrElse(Json.Null),
          "lang" -> req.lang.map(_.asJson).getOrElse(Json.Null)
        )
      )
      _ <- repos.demandsToReview.remove(NonEmptyList.of(req.id))
      _ <- repos.commands.addCreateResp(List(d))
    } yield ()

}
