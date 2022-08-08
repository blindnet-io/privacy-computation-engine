package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.db.DbUtil
import model.vocabulary.*
import model.vocabulary.terms.*
import io.blindnet.privacy.util.extension.*
import doobie.*
import doobie.implicits.*

trait PendingDemandsRepository {
  def getPendingDemandIds(appId: String): IO[List[String]]

  def storePendingDemand(appId: String, id: String): IO[Unit]
}

object PendingDemandsRepository {
  def live(xa: Transactor[IO]): PendingDemandsRepository =
    new PendingDemandsRepository {
      def getPendingDemandIds(appId: String): IO[List[String]] =
        sql"""
          select d.id
          from pending_demands pd
            join demands d on d.id = pd.did
            join privacy_requests pr on pr.id = d.prid
          where pr.appid = $appId::uuid
        """
          .query[String]
          .to[List]
          .transact(xa)

      def storePendingDemand(appId: String, id: String): IO[Unit] =
        val exists = sql"""select exists(select 1 from pending_demands where did = $id::uuid)"""
          .query[Boolean]
          .unique

        val store =
          sql"""
            insert into pending_demands
            values (gen_random_uuid(), $id::uuid)
          """.update.run

        val tr = for {
          ex <- exists
          _  <- if !ex then store else sql"select 1".query[Int].unique
        } yield ()

        tr.transact(xa).void

    }

}
