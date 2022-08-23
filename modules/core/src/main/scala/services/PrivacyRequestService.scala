package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
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

class PrivacyRequestService(
    repos: Repositories
) {

  private def validateRequest(req: PrivacyRequest) = {
    for {
      _ <-
        if req.demands.map(_.action).toSet.size == req.demands.size then IO.unit
        else "2 or more demands have duplicate action types".failBadRequest

      (invalid, _) = PrivacyRequest.validateDemands(req)

      _ <-
        if invalid.length == 0 then IO.unit
        else invalid.foldLeft("")((acc, cur) => acc + s"${cur._1.mkString_("\n")}").failBadRequest

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
        demands
      )

      _ <- validateRequest(pr)
      _ <- repos.privacyRequest.store(pr)
      _ <- repos.demandsToProcess.add(demands.map(_.id))

    } yield PrivacyRequestCreatedPayload(reqId)
  }

  def getRequestHistory(appId: UUID, userId: String) =
    for {
      reqIds <- repos.privacyRequest.getAllUserRequestIds(appId, userId)
      // TODO: this can be optimized in the db
      resps  <- reqIds.parTraverse(
        id =>
          for {
            req   <- repos.privacyRequest.getRequest(id)
            resps <- repos.privacyRequest.getResponsesForRequest(id)
          } yield req -> resps
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
      _         <- verifyReqExists(requestId, appId, userId)
      responses <- repos.privacyRequest.getResponsesForRequest(requestId)
      resp = responses.map(PrivacyResponsePayload.fromPrivPrivacyResponse)
    } yield resp

}
