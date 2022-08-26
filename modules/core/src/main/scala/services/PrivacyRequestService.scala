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
      _ <-
        if req.demands.map(_.action).toSet.size == req.demands.size then IO.unit
        else "2 or more demands have duplicate action types".failBadRequest

      (invalid, _) = PrivacyRequest.validateDemands(req)

      _ <-
        if invalid.length == 0 then IO.unit
        else invalid.foldLeft("")((acc, cur) => acc + s"${cur._1.mkString_("\n")}").failBadRequest

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

      _ <-
        if validConsentIds.exists(_ == false) then "Invalid consent id provided".failBadRequest
        else IO.unit

    } yield ()

  }

  def createPrivacyRequest(req: CreatePrivacyRequestPayload, appId: UUID) = {
    for {
      reqId     <- UUIDGen.randomUUID[IO]
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      demands = req.demands.zip(demandIds).map {
        case (d, id) => PrivacyRequestDemand.toPrivDemand(id, reqId, d)
      }

      timestamp <- Clock[IO].realTimeInstant
      ds        <- NonEmptyList.fromList(req.dataSubject) match {
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

    } yield PrivacyRequestCreatedPayload(reqId)
  }

  def getRequestHistory(appId: UUID, userId: String) =
    for {
      _      <- IO.unit
      reqIds <- repos.privacyRequest.getAllUserRequestIds(appId, userId)
      // TODO: this can be optimized in the db
      resps  <- reqIds.parTraverse(
        id =>
          val getReq  = repos.privacyRequest.getRequest(id)
          val getResp = repos.privacyRequest.getResponsesForRequest(id)
          (getReq both getResp).map { case (req, resps) => req -> resps }
      )

      history = resps.flatMap(
        r =>
          r._1.map(
            req => {
              val statuses                           = r._2.map(_.status)
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

              PrItem(req.id, req.timestamp, req.demands.length, status)
            }
          )
      )

    } yield RequestHistoryPayload(history)

  private def verifyReqExists(requestId: UUID, appId: UUID, userId: Option[String]) =
    repos.privacyRequest
      .requestExist(requestId, appId, userId)
      .flatMap(if _ then IO.unit else "Request not found".failNotFound)

  def getResponse(requestId: UUID, appId: UUID, userId: Option[String]) =
    for {
      _             <- verifyReqExists(requestId, appId, userId)
      privResponses <- repos.privacyRequest.getResponsesForRequest(requestId)
      resp = PrivacyResponse
        .group(privResponses)
        .map(PrivacyResponsePayload.fromPrivPrivacyResponse)
    } yield resp

}
