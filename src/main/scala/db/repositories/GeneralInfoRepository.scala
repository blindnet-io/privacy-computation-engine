package io.blindnet.privacy
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.db.DbUtil
import io.blindnet.privacy.util.extension.*
import model.vocabulary.*
import model.vocabulary.terms.*

trait GeneralInfoRepository {
  def getGeneralInfo(appId: UUID): IO[Option[GeneralInformation]]
}

object GeneralInfoRepository {
  def live(xa: Transactor[IO]): GeneralInfoRepository =
    new GeneralInfoRepository {
      def getGeneralInfo(appId: UUID): IO[Option[GeneralInformation]] =
        sql"""
          select countries, organization, dpo, data_consumer_categories, access_policies, privacy_policy_link, data_security_information
          from general_information
          where appid = $appId
        """
          .query[GeneralInformation]
          .option
          .transact(xa)

    }

}
