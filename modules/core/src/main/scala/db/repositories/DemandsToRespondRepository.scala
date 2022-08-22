package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.pce.db.DbUtil
import io.blindnet.pce.util.extension.*
import priv.*
import priv.terms.*
import io.blindnet.pce.model.DemandToRespond
import io.circe.*
import io.circe.parser.*
import org.postgresql.util.PGobject

trait DemandsToRespondRepository {
  def get(n: Int = 0): IO[List[DemandToRespond]]

  def add(ds: List[DemandToRespond]): IO[Unit]

  def remove(id: NonEmptyList[UUID]): IO[Unit]
}

object DemandsToRespondRepository {

  def live(xa: Transactor[IO]): DemandsToRespondRepository =
    new DemandsToRespondRepository {
      def get(n: Int = 0): IO[List[DemandToRespond]] =
        sql"""
          select d.id, pd.data
          from pending_demands_to_respond pd
            join demands d on d.id = pd.did
            join privacy_requests pr on pr.id = d.prid
          order by pr.date desc
        """
          .query[(UUID, String)]
          .map(r => DemandToRespond(r._1, parse(r._2).getOrElse(Json.Null)))
          .to[List]
          .transact(xa)

      def add(ds: List[DemandToRespond]): IO[Unit] =
        val sql = """
            insert into pending_demands_to_respond (did, data)
            values (?, ?)
          """
        Update[(UUID, String)](sql)
          .updateMany(ds.map(d => (d.dId, d.data.toString)))
          .transact(xa)
          .void

      def remove(ids: NonEmptyList[UUID]): IO[Unit] =
        (fr"""
          delete from pending_demands_to_respond
          where
        """ ++ Fragments.in(fr"did", ids)).update.run.transact(xa).void

    }

}
