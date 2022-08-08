package io.blindnet.privacy
package db.repositories.privacyrequest

import cats.data.NonEmptyList
import cats.effect.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.model.vocabulary.request.*
import model.vocabulary.*
import model.vocabulary.terms.*

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: String, appId: String, userId: String): IO[Boolean]

  def demandExist(appid: String, dId: String): IO[Boolean]

  def getRequest(reqId: String): IO[Option[PrivacyRequest]]

  def getRequestSimple(reqId: String): IO[Option[PrivacyRequest]]

  def getRequestsSimple(reqId: NonEmptyList[String]): IO[List[PrivacyRequest]]

  def getDemand(dId: String): IO[Option[Demand]]

  def getDemands(dIds: NonEmptyList[String]): IO[List[Demand]]

  def getResponse(reqId: String): IO[List[PrivacyResponse]]

  def getDemandResponse(dId: String): IO[Option[PrivacyResponse]]

  def storeNewResponse(r: PrivacyResponse): IO[Unit]

  def storeRecommendation(r: Recommendation): IO[Unit]

  def getRecommendation(dId: String): IO[Option[Recommendation]]

  def getAllUserRequestIds(appId: String, userId: String): IO[List[String]]
}

object PrivacyRequestRepository {

  def live(xa: Transactor[IO]): PrivacyRequestRepository =
    new PrivacyRequestRepositoryLive(xa)

}
