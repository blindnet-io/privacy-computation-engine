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

trait GeneralInfoRepository {
  def get(appId: UUID): IO[Option[GeneralInformation]]

  def upsert(appId: UUID, gi: GeneralInformation): IO[Unit]
}

object GeneralInfoRepository {
  def live(xa: Transactor[IO]): GeneralInfoRepository =
    new GeneralInfoRepository {
      def get(appId: UUID): IO[Option[GeneralInformation]] =
        sql"""
          select countries, organization, dpo, data_consumer_categories, privacy_policy_link, data_security_information
          from general_information
          where appid = $appId
        """
          .query[GeneralInformation]
          .option
          .transact(xa)

      def upsert(appId: UUID, gi: GeneralInformation): IO[Unit] = {

        val del    = sql"""delete from general_information where appId = $appId""".update.run
        val insert =
          sql"""
            insert into general_information
            values (gen_random_uuid(), $appId, ${gi.organization}, ${gi.dpo}, ${gi.countries},
            ${gi.dataConsumerCategories}, ${List("")}, ${gi.privacyPolicyLink},
            ${gi.dataSecurityInfo})
          """.update.run

        (del *> insert).transact(xa).void
      }

    }

}
