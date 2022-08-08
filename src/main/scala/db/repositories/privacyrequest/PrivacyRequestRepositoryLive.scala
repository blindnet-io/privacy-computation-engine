package io.blindnet.privacy
package db.repositories.privacyrequest

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.privacy.model.vocabulary.request.*
import model.vocabulary.*
import model.vocabulary.terms.*
import io.blindnet.privacy.util.extension.*
import io.blindnet.privacy.db.DbUtil.*

class PrivacyRequestRepositoryLive(xa: Transactor[IO]) extends PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit] = {
    def storePR =
      sql"""
        insert into privacy_requests (id, appid, dsid, date, target, email)
        values (${pr.id}::uuid, ${pr.appId}::uuid,
        ${pr.dataSubject.headOption.map(_.id)}::uuid, ${pr.timestamp},
        ${pr.target.encode}::target_terms, ${pr.email})
      """.update.run

    def storeDemand(d: Demand) =
      sql"""
        insert into demands (id, prid, action, message, lang)
        values (${d.id}::uuid, ${pr.id}::uuid, ${d.action.encode}::action_terms, ${d.message}, ${d.language})
      """.update.run

    // TODO: this is business logic. move it to service and handle transaction (Resource)
    def storeResponse(d: Demand, id: String) =
      sql"""
        insert into privacy_responses (id, did)
        values ($id::uuid, ${d.id}::uuid)
      """.update.run

    def storeResponseEvent(d: Demand, id: String, prId: String) =
      sql"""
        insert into privacy_response_events (id, prid, date, status, message, lang)
        values ($id::uuid, $prId::uuid, ${pr.timestamp}, ${Status.UnderReview.encode}::status_terms, null, null)
      """.update.run

    val store = for {
      r1 <- storePR
      r2 <- pr.demands.traverse(
        d =>
          for {
            r2a <- storeDemand(d)
            // TODO: where to generate IDs?
            id1 = UUID.randomUUID().toString
            id2 = UUID.randomUUID().toString
            r2b <- storeResponse(d, id1)
            r2c <- storeResponseEvent(d, id2, id1)
          } yield r2a + r2b + r2c
      )
    } yield r1 + r2.combineAll

    // TODO: ensure inserted
    store.transact(xa).void
  }

  def requestExist(reqId: String, appId: String, userId: String): IO[Boolean] =
    queries.requestExist(reqId, appId, userId).transact(xa)

  def demandExist(appId: String, dId: String): IO[Boolean] =
    queries.demandExist(appId, dId).transact(xa)

  def getRequest(reqId: String): IO[Option[PrivacyRequest]] = {
    val res =
      for {
        pr <- queries.getPrivacyRequest(reqId).toOptionT
        ds <- queries.getRequestDemands(reqId).map(_.some).toOptionT
      } yield pr.copy(demands = ds)

    res.value.transact(xa)
  }

  def getRequestSimple(reqId: String): IO[Option[PrivacyRequest]] =
    queries.getPrivacyRequest(reqId).transact(xa)

  def getRequestsSimple(reqIds: NonEmptyList[String]): IO[List[PrivacyRequest]] =
    queries.getPrivacyRequests(reqIds).transact(xa)

  def getDemand(dId: String): IO[Option[Demand]] =
    queries.getDemand(dId).transact(xa)

  def getDemands(dIds: NonEmptyList[String]): IO[List[Demand]] =
    queries.getDemands(dIds).transact(xa)

  def getResponse(reqId: String): IO[List[PrivacyResponse]] =
    queries.getAllDemandResponses(reqId).transact(xa)

  def getDemandResponse(dId: String): IO[Option[PrivacyResponse]] =
    queries.getDemandResponse(dId).transact(xa)

  def storeNewResponse(r: PrivacyResponse): IO[Unit] =
    queries.storeNewResponse(r).transact(xa).void

  def storeRecommendation(r: Recommendation): IO[Unit] =
    queries.storeRecommendation(r).transact(xa).void

  def getRecommendation(dId: String): IO[Option[Recommendation]] =
    queries.getRecommendation(dId).transact(xa)

  def getAllUserRequestIds(appId: String, userId: String): IO[List[String]] =
    queries.getAllUserRequestIds(appId, userId).transact(xa)

}
