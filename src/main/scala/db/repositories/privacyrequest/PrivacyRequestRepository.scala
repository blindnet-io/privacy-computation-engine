package io.blindnet.privacy
package db.repositories.privacyrequest

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.model.vocabulary.request.*
import model.vocabulary.*
import model.vocabulary.terms.*

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: UUID, appId: UUID, userId: String): IO[Boolean]

  def demandExist(appid: UUID, dId: UUID): IO[Boolean]

  def getRequest(reqId: UUID): IO[Option[PrivacyRequest]]

  def getRequestSimple(reqId: UUID): IO[Option[PrivacyRequest]]

  def getRequestsSimple(reqId: NonEmptyList[UUID]): IO[List[PrivacyRequest]]

  def getDemand(dId: UUID): IO[Option[Demand]]

  def getDemands(dIds: NonEmptyList[UUID]): IO[List[Demand]]

  def getResponse(reqId: UUID): IO[List[PrivacyResponse]]

  def getDemandResponse(dId: UUID): IO[Option[PrivacyResponse]]

  def storeNewResponse(r: PrivacyResponse): IO[Unit]

  def storeRecommendation(r: Recommendation): IO[Unit]

  def getRecommendation(dId: UUID): IO[Option[Recommendation]]

  def getAllUserRequestIds(appId: UUID, userId: String): IO[List[UUID]]
}

object PrivacyRequestRepository {

  def live(xa: Transactor[IO]): PrivacyRequestRepository =
    new PrivacyRequestRepositoryLive(xa)

}
