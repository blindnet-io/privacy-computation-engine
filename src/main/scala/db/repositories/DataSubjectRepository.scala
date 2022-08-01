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

trait DataSubjectRepository {
  def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean]
}

object DataSubjectRepository {
  def live(xa: Transactor[IO]): DataSubjectRepository =
    new DataSubjectRepository {

      def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean] =
        (fr"select count(*) from data_subjects where appid = $appId::uuid and"
          ++ Fragments.in(fr"id", userIds.map(_.id)))
          .query[Int]
          .unique
          .map(_ > 0)
          .transact(xa)

    }

}
