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
import cats.data.OptionT

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: String, appId: String, userId: String): IO[Boolean]

  def getRequest(reqId: String, appId: String, userId: String): IO[Option[PrivacyRequest]]

  def getResponse(reqId: String, appId: String, userId: String): IO[List[PrivacyResponse]]
}

object PrivacyRequestRepository {
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
                // they are not having a particular business value but this makes it harder to test
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

      def getRequest(reqId: String, appId: String, userId: String): IO[Option[PrivacyRequest]] = {
        val getReq =
          sql"""
            select id, appid, dsid, date, target, email
            from privacy_requests
            where id = $reqId::uuid and appid = $appId::uuid and dsid = $userId
          """
            .query[(String, String, String, Instant, Target, Option[String])]
            .option

        // TODO: restrictions
        val getDemands =
          sql"""
            select id, action, message, lang
            from demands
            where prid = $reqId
          """
            .query[(String, Action, Option[String], Option[String])]
            .map {
              case (id, action, msg, lang) => Demand(id, action, msg, lang, List.empty, List.empty)
            }
            .to[List]

        val res =
          for {
            (id, appId, dsid, time, target, email) <- OptionT(getReq)
            demands                                <- OptionT(getDemands.map(_.some))
          } yield PrivacyRequest(
            id,
            appId,
            time,
            target,
            email,
            List(DataSubject(dsid, "")),
            demands
          )

        res.value.transact(xa)
      }

      def getResponse(
          reqId: String,
          appId: String,
          userId: String
      ): IO[List[PrivacyResponse]] = {
        sql"""
            with query as (
              select pr.id as id, pre.id as eid, pre.date as date, d.action as action, pre.status as status,
                pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pre.data as data,
                ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
              from privacy_response_events pre
                join privacy_responses pr on pr.id = pre.prid
                join demands d on d.id = pr.did
              where d.prid = $reqId::uuid
            )
            select * from query where r = 1;
          """
          .query[
            (
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
            case (id, eid, t, a, s, answer, msg, lang, system, data) =>
              PrivacyResponse(
                id,
                eid,
                t,
                a,
                s,
                answer.flatMap(a => parse(a).toOption),
                msg,
                lang,
                system,
                List.empty,
                data
              )

          }
          .to[List]
          .transact(xa)
      }

    }

}
