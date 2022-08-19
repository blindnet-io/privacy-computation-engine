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

trait DemandsToProcessRepository {
  def get(n: Int = 0): IO[List[UUID]]

  def add(ids: List[UUID]): IO[Unit]

  def remove(id: NonEmptyList[UUID]): IO[Unit]
}

object DemandsToProcessRepository {
  def live(xa: Transactor[IO]): DemandsToProcessRepository =
    new DemandsToProcessRepository {
      def get(n: Int = 0): IO[List[UUID]] =
        sql"""
          select d.id
          from pending_demands_to_process pd
            join demands d on d.id = pd.did
            join privacy_requests pr on pr.id = d.prid
          order by pr.date desc
        """
          .query[UUID]
          .to[List]
          .transact(xa)

      def add(ids: List[UUID]): IO[Unit] =
        val sql = """
            insert into pending_demands_to_process
            values (?)
          """
        Update[UUID](sql).updateMany(ids).transact(xa).void

      def remove(ids: NonEmptyList[UUID]): IO[Unit] =
        (fr"""
          delete from pending_demands_to_process
          where
        """ ++ Fragments.in(fr"did", ids)).update.run.transact(xa).void

    }

}
