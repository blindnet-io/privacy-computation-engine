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
import io.blindnet.pce.api.endpoints.messages.bridge.*
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.priv.Recommendation
import io.blindnet.pce.util.extension.*
import io.circe.generic.auto.*
import io.circe.literal.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.terms.*
import io.blindnet.pce.priv.Timeline
import io.blindnet.pce.priv.PSContext

class BridgeService(
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
      _ <- repos.privacyRequest
        .demandExist(jwt.appId, dId)
        .onFalseNotFound(s"Demand $dId not found")
      d <- repos.privacyRequest.getDemand(dId, false).orFail(s"Demand $dId not found")
      rId = d.reqId
      req <- repos.privacyRequest.getRequest(rId, false).orFail(s"Request $rId not found")
      rec <- repos.privacyRequest.getRecommendation(jwt.appId, dId)
      res = PendingDemandDetailsPayload.fromPrivDemand(d, req, rec)
    } yield res

  def approveDemand(jwt: AppJwt)(req: ApproveDemandPayload) =
    for {
      _ <- repos.demandsToReview.exists(jwt.appId, req.id).onFalseBadRequest("Demand not pending")
      r <- repos.privacyRequest
        .getRecommendation(jwt.appId, req.id)
        .orBadRequest(s"No recommendation found")
      newR = r.copy(status = Some(Status.Granted), motive = None)
      _ <- repos.privacyRequest.updateRecommendation(newR)
      d <- CommandCreateResponse.create(
        req.id,
        json"""{
          "msg": ${req.msg},
          "lang": ${req.lang}
        }"""
      )
      _ <- repos.demandsToReview.remove(NonEmptyList.of(req.id))
      _ <- repos.commands.pushCreateResponse(List(d))
    } yield ()

  def denyDemand(jwt: AppJwt)(req: DenyDemandPayload) =
    for {
      _ <- repos.demandsToReview.exists(jwt.appId, req.id).onFalseBadRequest("Demand not pending")
      r <- repos.privacyRequest
        .getRecommendation(jwt.appId, req.id)
        .orBadRequest(s"No recommendation found")
      newR = r.copy(status = Some(Status.Denied), motive = Some(req.motive))
      _ <- repos.privacyRequest.updateRecommendation(newR)
      d <- CommandCreateResponse.create(
        req.id,
        json"""{
          "msg": ${req.msg},
          "lang": ${req.lang}
        }"""
      )
      _ <- repos.demandsToReview.remove(NonEmptyList.of(req.id))
      _ <- repos.commands.pushCreateResponse(List(d))
    } yield ()

  def changeRecommendation(jwt: AppJwt)(req: ChangeRecommendationPayload) =
    for {
      _ <- repos.demandsToReview
        .exists(jwt.appId, req.demandId)
        .onFalseBadRequest("Demand not pending")
      newRec = RecommendationPayload.toRecommendation(req.recommendation, req.demandId)
      newRecV <- Recommendation.validate(newRec).toEither.orBadRequest
      _       <- repos.privacyRequest.updateRecommendation(newRecV)
    } yield ()

  def getCompletedDemands(jwt: AppJwt)(x: Unit) =
    for {
      demands <- repos.privacyRequest.getCompletedDemands(jwt.appId)
      res = demands.map(CompletedDemandPayload.fromPrivCompletedDemand)
    } yield res

  // TODO: validation service
  private def verifyDemandExists(appId: UUID, dId: UUID) =
    repos.privacyRequest
      .demandExist(appId, dId)
      .onFalseNotFound("Demand not found")

  def getCompletedDemandInfo(jwt: AppJwt)(dId: UUID) =
    for {
      _         <- verifyDemandExists(jwt.appId, dId)
      d         <- repos.privacyRequest.getDemand(dId, false).orFail("Demand not found")
      req       <- repos.privacyRequest.getRequestFromDemand(d.id).orFail("Request not found")
      responses <- repos.privacyRequest.getDemandResponses(dId)
      resp = responses
        .filter(_.status.isAnswered)
        .map(r => CompletedDemandInfoPayload.fromPriv(d, req, r))
    } yield resp

  def getTimeline(jwt: AppJwt)(uId: String) =
    for {
      _ <- IO.unit
      ds = DataSubject(uId, jwt.appId)
      reqs  <- repos.privacyRequest.getRequestsForUser(ds)
      resps <- reqs
        .parTraverse(r => repos.privacyRequest.getResponsesForRequest(r.id))
        .map(_.flatten)
        .map(rs => PrivacyResponse.group(rs))

      timeline <- repos.events.getTimelineNoScope(ds)
      lbIds = timeline.events.flatMap(_.getLbId).toNel
      lbs <- lbIds.fold(IO(List.empty))(repos.legalBase.get(jwt.appId, _))
      res = TimelineEventsPayload.fromPriv(reqs, resps, timeline, lbs)
    } yield res

}
