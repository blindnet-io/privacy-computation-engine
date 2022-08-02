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

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]

  def requestExist(reqId: String): IO[Boolean]

  def getResponse(reqId: String): IO[Option[List[PrivacyResponse]]]
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

      def requestExist(reqId: String): IO[Boolean] =
        sql"""
            select exists (select 1 from privacy_requests pr where id = $reqId::uuid)
          """
          .query[Boolean]
          .unique
          .transact(xa)

      def getResponse(reqId: String): IO[Option[List[PrivacyResponse]]] = {
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
                String,
                String,
                Option[String],
                Option[String],
                Option[String],
                Option[String],
                Option[String]
            )
          ]
          .to[List]
          .map(_.map {
            case (id, eid, t, action, status, answer, msg, lang, system, data) =>
              for {
                a <- Action.parse(action).toOption
                s <- Status.parse(status).toOption
                j = answer.flatMap(a => parse(a).toOption)
              } yield PrivacyResponse(id, eid, t, a, s, j, msg, lang, system, List.empty, data)

          }.sequence)
          .transact(xa)
      }

    }

}
