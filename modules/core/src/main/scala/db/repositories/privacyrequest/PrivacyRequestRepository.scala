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

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: UUID, appId: UUID, userId: Option[String]): IO[Boolean]

  def demandExist(appid: UUID, dId: UUID): IO[Boolean]

  def getRequest(reqId: UUID, withDemands: Boolean = true): IO[Option[PrivacyRequest]]

  def getRequest(d: Demand): IO[Option[PrivacyRequest]]

  def getRequests(reqIds: NonEmptyList[UUID]): IO[List[PrivacyRequest]]

  def getDemand(dId: UUID, withRestrictions: Boolean = true): IO[Option[Demand]]

  def getDemands(dIds: NonEmptyList[UUID]): IO[List[Demand]]

  def getResponsesForRequest(reqId: UUID): IO[List[PrivacyResponse]]

  def getResponse(respId: UUID): IO[Option[PrivacyResponse]]

  def getDemandResponse(dId: UUID): IO[Option[PrivacyResponse]]

  def storeNewResponse(r: PrivacyResponse): IO[Unit]

  def storeResponseData(preId: UUID, data: Option[String]): IO[Unit]

  def storeRecommendation(r: Recommendation): IO[Unit]

  def getRecommendation(dId: UUID): IO[Option[Recommendation]]

  def getAllUserRequestIds(appId: UUID, userId: String): IO[List[UUID]]

}

object PrivacyRequestRepository {

  def live(xa: Transactor[IO]): PrivacyRequestRepository =
    new PrivacyRequestRepositoryLive(xa)

}
