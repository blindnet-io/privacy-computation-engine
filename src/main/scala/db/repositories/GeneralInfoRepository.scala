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

trait GeneralInfoRepository {
  def getGeneralInfo(appId: String): IO[Option[GeneralInformation]]

  def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean]
}

object GeneralInfoRepository {
  def live(xa: Transactor[IO]): GeneralInfoRepository =
    new GeneralInfoRepository {
      def getGeneralInfo(appId: String): IO[Option[GeneralInformation]] =
        sql"""
          select countries, organization, dpo, data_consumer_categories, access_policies, privacy_policy_link, data_security_information
          from general_information
          where appid = $appId::uuid
        """
          .query[GeneralInformation]
          .option
          .transact(xa)

      def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean] =
        (fr"select count(*) from data_subjects where appid = $appId::uuid and"
          ++ DbUtil.Fragments.inUuid(fr"id", userIds.map(_.id)))
          .query[Int]
          .unique
          .map(_ > 0)
          .transact(xa)

    }

}
