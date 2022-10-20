package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.identityclient.auth.*
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

  def getPendingDemands(jwt: AppJwt)(x: Unit) = {
    for {
      dIds    <- repos.demandsToReview.get(jwt.appId)
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

  def getPendingDemandDetails(jwt: AppJwt)(dId: UUID) =
    for {
      _ <- repos.privacyRequest.demandExist(jwt.appId, dId).onFalseNotFound(s"Demand $dId not found")
      d <- repos.privacyRequest.getDemand(dId, false).orFail(s"Demand $dId not found")
      rId = d.reqId
      req <- repos.privacyRequest.getRequest(rId, false).orFail(s"Request $rId not found")
      rec <- repos.privacyRequest.getRecommendation(dId)
      res = PendingDemandDetailsPayload.fromPrivDemand(d, req, rec)
    } yield res

  def approveDemand(jwt: AppJwt)(req: ApproveDemandPayload) =
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

  def denyDemand(jwt: AppJwt)(req: DenyDemandPayload) =
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

  def getCompletedDemands(appId: UUID) =
    for {
      demands <- repos.privacyRequest.getCompletedDemands()
      res = demands.map(CompletedDemandPayload.fromPrivCompletedDemand)
    } yield res

  // TODO: validation service
  private def verifyDemandExists(appId: UUID, dId: UUID) =
    repos.privacyRequest
      .demandExist(appId, dId)
      .onFalseNotFound("Demand not found")

  def getCompletedDemandInfo(appId: UUID, dId: UUID) =
    for {
      _         <- verifyDemandExists(appId, dId)
      d         <- repos.privacyRequest.getDemand(dId, false).orFail("Demand not found")
      req       <- repos.privacyRequest.getRequestFromDemand(d.id).orFail("Request not found")
      responses <- repos.privacyRequest.getDemandResponses(dId)
      resp = responses
        .filter(_.status.isAnswered)
        .map(r => CompletedDemandInfoPayload.fromPriv(d, req, r))
    } yield resp

}
