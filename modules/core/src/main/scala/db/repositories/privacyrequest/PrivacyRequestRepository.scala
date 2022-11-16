package io.blindnet.pce
package db.repositories.privacyrequest

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import doobie.util.transactor.Transactor
import priv.privacyrequest.*
import priv.*
import priv.terms.*

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest, responses: List[PrivacyResponse]): IO[Unit]

  def requestExist(reqId: RequestId, appId: UUID, userId: Option[String]): IO[Boolean]

  def demandExist(appid: UUID, dId: UUID): IO[Boolean]

  def demandExist(appid: UUID, dId: UUID, userId: String): IO[Boolean]

  def getRequest(reqId: RequestId, withDemands: Boolean = true): IO[Option[PrivacyRequest]]

  def getRequestFromDemand(dId: UUID): IO[Option[PrivacyRequest]]

  def getRequests(reqIds: NonEmptyList[RequestId]): IO[List[PrivacyRequest]]

  def getRequestsForUser(ds: DataSubject): IO[List[PrivacyRequest]]

  def getDemand(dId: UUID, withRestrictions: Boolean = true): IO[Option[Demand]]

  def getDemands(dIds: NonEmptyList[UUID]): IO[List[Demand]]

  def getCompletedDemands(appid: UUID): IO[List[CompletedDemand]]

  def getResponsesForRequest(reqId: RequestId): IO[List[PrivacyResponse]]

  def getDemandResponses(dId: UUID): IO[List[PrivacyResponse]]

  def storeNewResponse(r: PrivacyResponse): IO[Unit]

  def storeResponseData(preId: ResponseEventId, data: Option[String]): IO[Unit]

  def storeRecommendation(r: Recommendation): IO[Unit]

  def updateRecommendation(r: Recommendation): IO[Unit]

  def getRecommendation(dId: UUID): IO[Option[Recommendation]]

  def getRecommendation(appId: UUID, dId: UUID): IO[Option[Recommendation]]

  def getAllUserRequestIds(ds: DataSubject): IO[List[RequestId]]

}

object PrivacyRequestRepository {

  def live(xa: Transactor[IO]): PrivacyRequestRepository =
    new PrivacyRequestRepositoryLive(xa)

}
