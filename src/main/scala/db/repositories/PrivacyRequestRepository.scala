package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.terms.*
import db.DbUtil
import javax.xml.crypto.Data
import io.blindnet.privacy.model.vocabulary.request.PrivacyRequest
import io.blindnet.privacy.model.vocabulary.request.Demand
import cats.effect.std.Random
import java.util.UUID

trait PrivacyRequestRepository {

  def store(pr: PrivacyRequest): IO[Unit]
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

    }

}
