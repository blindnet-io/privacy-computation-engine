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

trait DataSubjectRepository {
  def get(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Option[DataSubject]]

  def exist(appId: UUID, id: String): IO[Boolean]
}

object DataSubjectRepository {
  def live(xa: Transactor[IO]): DataSubjectRepository =
    new DataSubjectRepository {

      def get(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Option[DataSubject]] =
        (fr"select id, schema from data_subjects where appid = $appId and"
          ++ Fragments.in(fr"id", userIds.map(_.id)))
          .query[DataSubject]
          .option
          .transact(xa)

      def exist(appId: UUID, id: String): IO[Boolean] =
        sql"select count(*) from data_subjects where appid = $appId and id = $id"
          .query[Int]
          .map(_ > 0)
          .unique
          .transact(xa)

    }

}
