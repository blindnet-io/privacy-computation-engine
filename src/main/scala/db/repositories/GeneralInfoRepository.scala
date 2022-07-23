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
import model.vocabulary.general.*
import model.vocabulary.general.*
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
        // sql"""
        //   select countries, organizations, dpo, data_consumer_categories, access_policies, privacy_policy_link, data_security_information
        //   from general_information_view
        //   where appid = $appId
        // """
        //   .query[GeneralInformation]
        //   .unique
        //   .transact(xa)

        val res = for {
          gi <-
            sql"""
                select id, countries, data_consumer_categories, access_policies, privacy_policy_link, data_security_information
                from general_information
                where appid = $appId::uuid
              """
              .query[
                (
                    String,
                    Option[List[String]],
                    Option[List[String]],
                    Option[List[String]],
                    Option[String],
                    Option[String]
                )
              ]
              .map(
                r =>
                  (
                    r._1,
                    r._2.getOrElse(Nil),
                    r._3.getOrElse(Nil),
                    r._4.getOrElse(Nil),
                    r._5,
                    r._6
                  )
              )
              .option
              .toOptionT

          orgs <-
            sql"select name from general_information_organization gio where gio.gid = ${gi._1}::uuid"
              .query[Organization]
              .to[List]
              .toOptionT

          dpos <- sql"select name, contact from dpo where dpo.gid = ${gi._1}::uuid"
            .query[Dpo]
            .to[List]
            .toOptionT

        } yield GeneralInformation(gi._2, orgs, dpos, gi._3, gi._4, gi._5, gi._6)

        res.transact(xa).value

      def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean] =
        (fr"select count(*) from data_subjects where appid = $appId::uuid and"
          ++ DbUtil.Fragments.inUuid(fr"id", userIds.map(_.id)))
          .query[Int]
          .unique
          .map(_ > 0)
          .transact(xa)

    }

}
