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
import io.blindnet.pce.util.extension.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*
import codecs.given

class PrivacyRequestRepositoryLive(xa: Transactor[IO]) extends PrivacyRequestRepository {

  def store(pr: PrivacyRequest, responses: List[PrivacyResponse]): IO[Unit] = {
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

    def storeResponse(r: PrivacyResponse) =
      sql"""
        insert into privacy_responses (id, did, parent, action)
        values (${r.id}, ${r.demandId}, ${r.parent}, ${r.action.encode}::action_terms)
      """.update.run

    def storeResponseEvent(r: PrivacyResponse) =
      sql"""
        insert into privacy_response_events (id, prid, date, status)
        values (${r.eventId}, ${r.id}, ${pr.timestamp}, ${r.status.encode}::status_terms)
      """.update.run

    val store = for {
      _  <- storePR
      _  <- pr.demands.traverse(
        d =>
          for {
            r2a <- storeDemand(d)
            _   <- d.restrictions.traverse(r => storeRestriction(UUID.randomUUID(), d.id, r))
          } yield ()
      )
      r3 <- responses.traverse(
        r => {
          def store(r: PrivacyResponse) =
            storeResponse(r) >> storeResponseEvent(r)

          store(r) >> r.includes.traverse(store)
        }
      )
    } yield ()

    store.transact(xa).void
  }

  def requestExist(reqId: RequestId, appId: UUID, userId: Option[String]): IO[Boolean] =
    queries.requestExist(reqId, appId, userId).transact(xa)

  def demandExist(appId: UUID, dId: UUID): IO[Boolean] =
    queries.demandExist(appId, dId).transact(xa)

  def demandExist(appId: UUID, dId: UUID, userId: String): IO[Boolean] =
    queries.demandExist(appId, dId, userId).transact(xa)

  def getRequest(reqId: RequestId, withDemands: Boolean): IO[Option[PrivacyRequest]] = {
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

  def getRequests(reqIds: NonEmptyList[RequestId]): IO[List[PrivacyRequest]] =
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

  def getCompletedDemands(): IO[List[CompletedDemand]] =
    queries.getCompletedDemands().transact(xa)

  def getResponsesForRequest(reqId: RequestId): IO[List[PrivacyResponse]] =
    queries.getAllDemandResponses(reqId).transact(xa)

  def getDemandResponses(dId: UUID): IO[List[PrivacyResponse]] = {
    queries.getDemandResponses(dId).transact(xa)
  }

  def storeNewResponse(r: PrivacyResponse): IO[Unit] =
    queries.storeNewResponse(r).transact(xa).void

  def storeResponseData(preId: ResponseEventId, data: Option[String]): IO[Unit] =
    queries.storeResponseData(preId, data).transact(xa).void

  def storeRecommendation(r: Recommendation): IO[Unit] =
    queries.storeRecommendation(r).transact(xa).void

  def updateRecommendation(r: Recommendation): IO[Unit] =
    queries.updateRecommendation(r).transact(xa).void

  def getRecommendation(dId: UUID): IO[Option[Recommendation]] =
    queries.getRecommendation(dId).transact(xa)

  def getAllUserRequestIds(ds: DataSubject): IO[List[RequestId]] =
    queries.getAllUserRequestIds(ds).transact(xa)

}
