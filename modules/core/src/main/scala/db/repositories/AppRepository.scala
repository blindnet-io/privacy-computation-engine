package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.PCEApp

trait AppRepository {
  def get(id: UUID): IO[Option[PCEApp]]
}

object AppRepository {
  def live(xa: Transactor[IO]): AppRepository =
    new AppRepository {

      def get(id: UUID): IO[Option[PCEApp]] =
        sql"""
          select a.id, d.uri, arc.auto_transparency, arc.auto_consents, arc.auto_access, arc.auto_delete
          from apps a
            join dac d on d.appid = a.id
            join automatic_responses_config arc on arc.appid = a.id
          where a.id = $id
        """
          .query[PCEApp]
          .option
          .transact(xa)

    }

}
