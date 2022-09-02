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
  def exist(appId: UUID, id: String): IO[Boolean]

  def get(appId: UUID, id: String): IO[Option[DataSubject]]

  def get(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Option[DataSubject]]

  def insert(appId: UUID, ds: DataSubject): IO[Unit]
}

object DataSubjectRepository {
  def live(xa: Transactor[IO]): DataSubjectRepository =
    new DataSubjectRepository {

      def exist(appId: UUID, id: String): IO[Boolean] =
        sql"select count(*) from data_subjects where appid = $appId and id = $id"
          .query[Int]
          .map(_ > 0)
          .unique
          .transact(xa)

      def get(appId: UUID, id: String): IO[Option[DataSubject]] =
        sql"""
          select ds.id, ds.appid, ds.schema
          from data_subjects ds
            join privacy_requests pr on pr.dsid = ds.id
            join demands d on d.prid = pr.id
          where d.id = $id and d.appid = $appId
        """
          .query[DataSubject]
          .option
          .transact(xa)

      def get(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Option[DataSubject]] =
        (fr"select id, schema from data_subjects where appid = $appId and"
          ++ Fragments.in(fr"id", userIds.map(_.id)))
          .query[DataSubject]
          .option
          .transact(xa)

      def insert(appId: UUID, ds: DataSubject): IO[Unit] =
        sql"insert into data_subjects values (${ds.id}, ${appId}, ${ds.schema})".update.run
          .transact(xa)
          .void

    }

}
