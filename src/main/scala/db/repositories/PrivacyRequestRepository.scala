package io.blindnet.privacy
package db.repositories

import java.time.Instant
import java.util.UUID
import javax.xml.crypto.Data

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.Random
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.privacy.model.vocabulary.request.{ Demand, PrivacyRequest, PrivacyResponse }
import io.circe.*
import io.circe.parser.*
import model.vocabulary.*
import model.vocabulary.terms.*
import io.blindnet.privacy.util.extension.*
import io.blindnet.privacy.db.DbUtil.*

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: String, appId: String, userId: String): IO[Boolean]

  def getRequest(reqId: String): IO[Option[PrivacyRequest]]

  def getRequestSimple(reqId: String): IO[Option[PrivacyRequest]]

  def getRequestsSimple(reqId: NonEmptyList[String]): IO[List[PrivacyRequest]]

  def getDemand(dId: String): IO[Option[Demand]]

  def getDemands(dIds: NonEmptyList[String]): IO[List[Demand]]

  def getResponse(reqId: String): IO[List[PrivacyResponse]]

  def getDemandResponse(dId: String): IO[Option[PrivacyResponse]]

  def storeNewResponse(r: PrivacyResponse): IO[Unit]

  def getRequestsForUser(appId: String, userId: String): IO[List[String]]
}

object PrivacyRequestRepository {
  given Read[PrivacyResponse] =
    Read[
      (
          String,
          String,
          String,
          Instant,
          Action,
          Status,
          Option[String],
          Option[String],
          Option[String],
          Option[String],
          Option[String]
      )
    ]
      .map {
        case (id, prid, did, t, a, s, answer, msg, lang, system, data) =>
          val answ = answer.flatMap(a => parse(a).toOption)
          val incl = List.empty
          PrivacyResponse(id, prid, did, t, a, s, answ, msg, lang, system, incl, data)
      }

  given Read[PrivacyRequest] =
    Read[(String, String, String, Instant, Target, Option[String])]
      .map {
        case (id, appId, dsid, t, trg, email) =>
          val ds = List(DataSubject(dsid, ""))
          PrivacyRequest(id, appId, t, trg, email, ds, List.empty)
      }

  given Read[Demand] =
    Read[(String, String, Action, Option[String], Option[String])]
      .map { case (id, rid, a, m, l) => Demand(id, rid, a, m, l, List.empty, List.empty) }

  object queries {
    def getDemand(id: String) =
      sql"""
        select d.id, pr.id, action, message, lang
        from demands d
          join privacy_requests pr on pr.id = d.prid
        where d.id = $id::uuid
      """
        .query[Demand]
        .option

    def getDemands(ids: NonEmptyList[String]) =
      (sql"""
        select d.id, pr.id, action, message, lang
          from demands d
            join privacy_requests pr on pr.id = d.prid
          where
        """
        ++ FragmentsC.inUuid(fr"d.id", ids))
        .query[Demand]
        .to[List]

    def getRequestDemands(reqId: String) =
      sql"""
        select d.id, pr.id, action, message, lang
        from demands d
          join privacy_requests pr on pr.id = d.prid
        where prid = $reqId::uuid
      """
        .query[Demand]
        .to[List]

    def getPrivacyRequest(reqId: String) =
      sql"""
        select id, appid, dsid, date, target, email
        from privacy_requests
        where id = $reqId::uuid
      """
        .query[PrivacyRequest]
        .option

    def getPrivacyRequests(ids: NonEmptyList[String]) =
      (sql"""
        select id, appid, dsid, date, target, email
        from privacy_requests
        where
      """
        ++ FragmentsC.inUuid(fr"id", ids))
        .query[PrivacyRequest]
        .to[List]

  }

  def live(xa: Transactor[IO]): PrivacyRequestRepository =
    new PrivacyRequestRepository {

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
            values (gen_random_uuid(), ${prId}::uuid, ${pr.timestamp}, ${Status.UnderReview.encode}::status_terms, null, null)
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
        sql"""
          select exists (
            select 1 from privacy_requests pr
            where id = $reqId::uuid and appid = $appId::uuid and dsid = $userId
          )
        """
          .query[Boolean]
          .unique
          .transact(xa)

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
        sql"""
            with query as (
              select pre.id as id, pr.id as prid, d.id as did, pre.date as date, d.action as action, pre.status as status,
                pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pre.data as data,
                ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
              from privacy_response_events pre
                join privacy_responses pr on pr.id = pre.prid
                join demands d on d.id = pr.did
              where d.prid = $reqId::uuid
            )
            select * from query where r = 1;
          """
          .query[PrivacyResponse]
          .to[List]
          .transact(xa)

      def getDemandResponse(demandId: String): IO[Option[PrivacyResponse]] =
        // TODO: duplicate code
        sql"""
          with query as (
            select pre.id as id, pr.id as prid, d.id as did, pre.date as date, d.action as action, pre.status as status,
              pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pre.data as data,
              ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
            from privacy_response_events pre
              join privacy_responses pr on pr.id = pre.prid
              join demands d on d.id = pr.did
            where d.id = $demandId::uuid
          )
          select * from query where r = 1;
        """
          .query[PrivacyResponse]
          .option
          .transact(xa)

      def storeNewResponse(r: PrivacyResponse): IO[Unit] =
        sql"""
          insert into privacy_response_events (id, prid, date, status, message, lang, data, answer)
          values (
            ${r.id}::uuid, ${r.responseId}::uuid, ${r.timestamp}, ${r.status.encode}::status_terms,
            ${r.message}, ${r.lang}, ${r.data}, ${r.answer.map(_.toString)}
          )
        """.update.run
          .transact(xa)
          .void

      def getRequestsForUser(appId: String, userId: String): IO[List[String]] =
        sql"""
          select id
          from privacy_requests pr
          where pr.dsid = ${userId} and pr.appid = ${appId}::uuid
        """
          .query[String]
          .to[List]
          .transact(xa)

    }

}
