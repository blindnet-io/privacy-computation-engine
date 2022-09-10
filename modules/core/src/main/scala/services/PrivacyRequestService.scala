package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.pce.model.error.given
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
import util.*
import io.blindnet.pce.model.*

class PrivacyRequestService(
    repos: Repositories
) {

  private def validateRequest(appId: UUID, req: PrivacyRequest) = {
    for {
      _ <- IO.unit
      // TODO: rethink if/how to handle duplicate action types
      // _ <- (req.demands.map(_.action).toSet.size == req.demands.size)
      //   .onFalseBadRequest("2 or more demands have duplicate action types")

      (invalid, _) = PrivacyRequest.validateDemands(req)

      _ <- (invalid.length == 0)
        .onFalseBadRequest(invalid.foldLeft("")((acc, cur) => acc + s"${cur._1.mkString_("\n")}"))

      validConsentIds <-
        req.demands.traverse(
          d =>
            d.restrictions.find(r => r.isInstanceOf[Restriction.Consent]) match {
              case Some(r) =>
                val cId = r.asInstanceOf[Restriction.Consent].consentId
                for {
                  lbOpt <- repos.legalBase.get(appId, cId, false)
                  isConsent = lbOpt.map(_.isConsent).getOrElse(false)
                } yield isConsent
              case None    => IO(true)
            }
        )

      _ <- validConsentIds.forall(_ == true).onFalseBadRequest("Invalid consent id provided")

    } yield ()

  }

  def createPrivacyRequest(req: CreatePrivacyRequestPayload, appId: UUID) = {
    for {
      reqId     <- UUIDGen.randomUUID[IO].map(RequestId.apply)
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      demands = req.demands.zip(demandIds).map {
        case (d, id) => PrivacyRequestDemand.toPrivDemand(id, reqId, d)
      }

      timestamp <- Clock[IO].realTimeInstant
      ds        <-
        NonEmptyList.fromList(req.dataSubject.map(dsp => dsp.toPrivDataSubject(appId))) match {
          case None             => IO(None)
          case Some(identities) => repos.dataSubject.get(appId, identities)
        }
      pr = PrivacyRequest(
        reqId,
        appId,
        timestamp,
        req.target.getOrElse(Target.System),
        req.email,
        ds,
        req.dataSubject.map(_.id),
        demands
      )
      _         <- validateRequest(appId, pr)

      responses <- PrivacyResponse.fromPrivacyRequest[IO](pr)
      _         <- repos.privacyRequest.store(pr, responses)
      cs        <- demands.traverse(d => CommandCreateRecommendation.create(d.id))
      _         <- repos.commands.addCreateRec(cs)

    } yield PrivacyRequestCreatedPayload(reqId.value)
  }

  def createReqHistoryItem(req: PrivacyRequest, resp: List[PrivacyResponse]) = {
    val statuses                           = resp.map(_.status)
    val (underReview, canceled, completed) =
      statuses.foldLeft((0, 0, 0))(
        (acc, cur) =>
          if (cur == Status.UnderReview) then (acc._1 + 1, acc._2, acc._3)
          else if (cur == Status.Canceled) then (acc._1, acc._2 + 1, acc._3)
          else (acc._1, acc._2, acc._3 + 1)
      )

    val l      = statuses.length
    val status =
      if (l == 0) then PrStatus.Completed
      else if (canceled == l) then PrStatus.Canceled
      else if (completed + canceled == l) then PrStatus.Completed
      else if (underReview == l) then PrStatus.InProcessing
      else if (underReview + canceled == l) then PrStatus.InProcessing
      else PrStatus.PartiallyCompleted

    PrItem(req.id.value, req.timestamp, req.demands.length, status)
  }

  def getRequestHistory(appId: UUID, userId: String) =
    for {
      ds     <- IO(DataSubject(userId, appId))
      reqIds <- repos.privacyRequest.getAllUserRequestIds(ds)
      // TODO: this can be optimized in the db
      reqs   <- reqIds
        .parTraverse(
          id =>
            val getReq  = repos.privacyRequest.getRequest(id)
            val getResp = repos.privacyRequest.getResponsesForRequest(id)
            getReq both getResp
        )
        .map(_.flatMap(r => r._1.map((_, r._2))))

      history = reqs.map { case (req, resp) => createReqHistoryItem(req, resp) }
    } yield RequestHistoryPayload(history)

  private def verifyReqExists(requestId: RequestId, appId: UUID, userId: Option[String]) =
    repos.privacyRequest
      .requestExist(requestId, appId, userId)
      .onFalseNotFound("Request not found")

  def getResponse(requestId: RequestId, appId: UUID, userId: Option[String]) =
    for {
      _             <- verifyReqExists(requestId, appId, userId)
      privResponses <- repos.privacyRequest.getResponsesForRequest(requestId)
      resp = PrivacyResponse
        .group(privResponses)
        .map(PrivacyResponsePayload.fromPrivPrivacyResponse)
    } yield resp

}
