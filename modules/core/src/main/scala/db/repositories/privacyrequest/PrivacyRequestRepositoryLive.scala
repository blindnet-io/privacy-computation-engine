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
        insert into privacy_requests (id, appid, dsid, provided_dsids, date, target, email)
        values (${pr.id}, ${pr.appId}, ${pr.dataSubject.map(_.id)}, ${pr.providedDsIds},
        ${pr.timestamp}, ${pr.target.encode}::target_terms, ${pr.email})
      """.update.run

    def storeDemand(d: Demand) =
      sql"""
        insert into demands (id, prid, action, message, lang)
        values (${d.id}, ${pr.id}, ${d.action.encode}::action_terms, ${d.message}, ${d.language})
      """.update.run

    def storeRestriction(id: UUID, dId: UUID, r: Restriction) =
      r match {
        case r: Restriction.PrivacyScope => {
          val insertRestriction =
            sql"""
              insert into demand_restrictions (id, did, type, cid, from_date, to_date, provenance_term, target_term, data_reference)
              values ($id, $dId, 'PRIVACY_SCOPE', null, null, null, null, null, null)
            """

          val insertRScope =
            fr"""
              insert into demand_restriction_scope
                select $id, s.id
                from "scope" s
                join data_categories dc ON dc.id = s.dcid
                join processing_categories pc ON pc.id = s.pcid
                join processing_purposes pp ON pp.id = s.ppid
                where
            """ ++ Fragments.or(
              r.scope.triples
                .map(
                  t =>
                    fr"dc.term = ${t.dataCategory.term} and" ++
                      fr"pc.term = ${t.processingCategory.term} and" ++
                      fr"pp.term = ${t.purpose.term}"
                )
                .toList*
            )

          (insertRestriction.update.run *> insertRScope.update.run)
        }

        case _ => {
          val values = r match {
            case r: Restriction.Consent       =>
              fr"($id, $dId, 'CONSENT', ${r.consentId}, null, null, null, null, null)"
            case r: Restriction.DateRange     =>
              fr"($id, $dId, 'DATE_RANGE', null, ${r.from}, ${r.to}, null, null, null)"
            case r: Restriction.Provenance    =>
              fr"""($id, $dId, 'PROVENANCE', null, null, null, ${r.term.encode}::provenance_terms,
              ${r.target.map(_.encode)}::target_terms, null)"""
            case r: Restriction.DataReference =>
              fr"($id, $dId, 'DATA_REFERENCE', null, null, null, null, null, ${r.dataReferences})"
            case _                            => fr"()"
          }

          (fr"""
            insert into demand_restrictions (id, did, type, cid, from_date, to_date, provenance_term, target_term, data_reference)
            values""" ++ values).update.run
        }
      }

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
            _   <- d.restrictions.traverse(r => storeRestriction(UUID.randomUUID(), d.id, r))
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

  def requestExist(reqId: UUID, appId: UUID, userId: Option[String]): IO[Boolean] =
    queries.requestExist(reqId, appId, userId).transact(xa)

  def demandExist(appId: UUID, dId: UUID): IO[Boolean] =
    queries.demandExist(appId, dId).transact(xa)

  def getRequest(reqId: UUID, withDemands: Boolean): IO[Option[PrivacyRequest]] = {
    val withD =
      for {
        pr <- queries.getPrivacyRequest(reqId).toOptionT
        ds <- queries.getRequestDemands(reqId).map(_.some).toOptionT
      } yield pr.copy(demands = ds)

    val res = if withDemands then withD.value else queries.getPrivacyRequest(reqId)
    res.transact(xa)
  }

  def getRequest(d: Demand): IO[Option[PrivacyRequest]] =
    queries.getPrivacyRequestFromDemand(d.id).transact(xa)

  def getRequests(reqIds: NonEmptyList[UUID]): IO[List[PrivacyRequest]] =
    queries.getPrivacyRequests(reqIds).transact(xa)

  def getDemand(dId: UUID, withRestrictions: Boolean = true): IO[Option[Demand]] = {
    val withR =
      for {
        d <- queries.getDemand(dId).toOptionT
        r <- queries.getDemandRestrictions(dId).toOptionT
        res = d.copy(restrictions = r)
      } yield res

    val res = if withRestrictions then withR.value else queries.getDemand(dId)
    res.transact(xa)
  }

  def getDemands(dIds: NonEmptyList[UUID]): IO[List[Demand]] =
    queries.getDemands(dIds).transact(xa)

  def getResponsesForRequest(reqId: UUID): IO[List[PrivacyResponse]] =
    queries.getAllDemandResponses(reqId).transact(xa)

  def getResponse(respId: UUID): IO[Option[PrivacyResponse]] =
    queries.getResponse(respId).transact(xa)

  def getDemandResponse(dId: UUID): IO[Option[PrivacyResponse]] =
    queries.getDemandResponse(dId).transact(xa)

  def storeNewResponse(r: PrivacyResponse): IO[Unit] =
    queries.storeNewResponse(r).transact(xa).void

  def storeResponseData(preId: UUID, data: Option[String]): IO[Unit] =
    queries.storeResponseData(preId, data).transact(xa).void

  def storeRecommendation(r: Recommendation): IO[Unit] =
    queries.storeRecommendation(r).transact(xa).void

  def getRecommendation(dId: UUID): IO[Option[Recommendation]] =
    queries.getRecommendation(dId).transact(xa)

  def getAllUserRequestIds(appId: UUID, userId: String): IO[List[UUID]] =
    queries.getAllUserRequestIds(appId, userId).transact(xa)

}
