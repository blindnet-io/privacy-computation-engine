package io.blindnet.pce
package db.repositories.privacyrequest

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.db.DbUtil.*
import priv.privacyrequest.*
import io.blindnet.pce.util.extension.*
import priv.*
import priv.terms.*

class PrivacyRequestRepositoryLive(xa: Transactor[IO]) extends PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit] = {
    def storePR =
      sql"""
        insert into privacy_requests (id, appid, dsid, date, target, email)
        values (${pr.id}, ${pr.appId}, ${pr.dataSubject.headOption.map(_.id)}, ${pr.timestamp},
        ${pr.target.encode}::target_terms, ${pr.email})
      """.update.run

    def storeDemand(d: Demand) =
      sql"""
        insert into demands (id, prid, action, message, lang)
        values (${d.id}, ${pr.id}, ${d.action.encode}::action_terms, ${d.message}, ${d.language})
      """.update.run

    // TODO: this is business logic. move it to service and handle transaction (Resource)
    def storeResponse(d: Demand, id: UUID) =
      sql"""
        insert into privacy_responses (id, did)
        values ($id, ${d.id})
      """.update.run

    def storeResponseEvent(d: Demand, id: UUID, prId: UUID) =
      sql"""
        insert into privacy_response_events (id, prid, date, status, message, lang)
        values ($id, $prId, ${pr.timestamp}, ${Status.UnderReview.encode}::status_terms, null, null)
      """.update.run

    val store = for {
      r1 <- storePR
      r2 <- pr.demands.traverse(
        d =>
          for {
            r2a <- storeDemand(d)
            // TODO: where to generate IDs?
            id1 = UUID.randomUUID()
            id2 = UUID.randomUUID()
            r2b <- storeResponse(d, id1)
            r2c <- storeResponseEvent(d, id2, id1)
          } yield r2a + r2b + r2c
      )
    } yield r1 + r2.combineAll

    // TODO: ensure inserted
    store.transact(xa).void
  }

  def requestExist(reqId: UUID, appId: UUID, userId: String): IO[Boolean] =
    queries.requestExist(reqId, appId, userId).transact(xa)

  def demandExist(appId: UUID, dId: UUID): IO[Boolean] =
    queries.demandExist(appId, dId).transact(xa)

  def getRequest(reqId: UUID): IO[Option[PrivacyRequest]] = {
    val res =
      for {
        pr <- queries.getPrivacyRequest(reqId).toOptionT
        ds <- queries.getRequestDemands(reqId).map(_.some).toOptionT
      } yield pr.copy(demands = ds)

    res.value.transact(xa)
  }

  def getRequestSimple(reqId: UUID): IO[Option[PrivacyRequest]] =
    queries.getPrivacyRequest(reqId).transact(xa)

  def getRequestsSimple(reqIds: NonEmptyList[UUID]): IO[List[PrivacyRequest]] =
    queries.getPrivacyRequests(reqIds).transact(xa)

  def getDemand(dId: UUID): IO[Option[Demand]] =
    queries.getDemand(dId).transact(xa)

  def getDemands(dIds: NonEmptyList[UUID]): IO[List[Demand]] =
    queries.getDemands(dIds).transact(xa)

  def getResponse(reqId: UUID): IO[List[PrivacyResponse]] =
    queries.getAllDemandResponses(reqId).transact(xa)

  def getDemandResponse(dId: UUID): IO[Option[PrivacyResponse]] =
    queries.getDemandResponse(dId).transact(xa)

  def storeNewResponse(r: PrivacyResponse): IO[Unit] =
    queries.storeNewResponse(r).transact(xa).void

  def storeRecommendation(r: Recommendation): IO[Unit] =
    queries.storeRecommendation(r).transact(xa).void

  def getRecommendation(dId: UUID): IO[Option[Recommendation]] =
    queries.getRecommendation(dId).transact(xa)

  def getAllUserRequestIds(appId: UUID, userId: String): IO[List[UUID]] =
    queries.getAllUserRequestIds(appId, userId).transact(xa)

}
