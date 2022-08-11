package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.pce.db.DbUtil
import io.blindnet.pce.util.extension.*
import priv.*
import priv.terms.*

trait PendingDemandsRepository {
  def getPendingDemandIds(appId: UUID): IO[List[UUID]]

  def storePendingDemand(appId: UUID, id: UUID): IO[Unit]

  def removePendingDemand(appId: UUID, id: UUID): IO[Unit]
}

object PendingDemandsRepository {
  def live(xa: Transactor[IO]): PendingDemandsRepository =
    new PendingDemandsRepository {
      def getPendingDemandIds(appId: UUID): IO[List[UUID]] =
        sql"""
          select d.id
          from pending_demands pd
            join demands d on d.id = pd.did
            join privacy_requests pr on pr.id = d.prid
          where pr.appid = $appId
        """
          .query[UUID]
          .to[List]
          .transact(xa)

      def storePendingDemand(appId: UUID, id: UUID): IO[Unit] =
        val exists = sql"""select exists(select 1 from pending_demands where did = $id)"""
          .query[Boolean]
          .unique

        val store =
          sql"""
            insert into pending_demands
            values (gen_random_uuid(), $id)
          """.update.run

        val tr = for {
          ex <- exists
          _  <- if !ex then store else sql"select 1".query[Int].unique
        } yield ()

        tr.transact(xa).void

      def removePendingDemand(appId: UUID, id: UUID): IO[Unit] =
        sql"""
          delete from pending_demands
          where id = $id
        """.update.run.transact(xa).void

    }

}
