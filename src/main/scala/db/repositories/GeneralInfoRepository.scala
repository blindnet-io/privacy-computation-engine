package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait GeneralInfoRepository {
  def getGeneralInfo(appId: String): IO[GeneralInformation]

  def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean]
}

object GeneralInfoRepository {
  def live(xa: Transactor[IO]): GeneralInfoRepository =
    new GeneralInfoRepository {
      def getGeneralInfo(appId: String): IO[GeneralInformation] =
        val res = for {
          gi   <-
            sql"""
                select id, countries, data_consumer_categories, access_policies, privacy_policy_link, data_security_information
                from general_information
                where appid = $appId
              """
              .query[(String, List[String], List[String], List[String], String, String)]
              .unique
          orgs <-
            sql"select name from general_information_organization gio where gio.gid = ${gi._1}"
              .query[Organization]
              .to[List]
          dpos <- sql"select name, contact from dpo where dop.gid = ${gi._1}"
            .query[Dpo]
            .to[List]
        } yield GeneralInformation(gi._2, orgs, dpos, gi._3, gi._4, gi._5, gi._6)
        res.transact(xa)

      def known(appId: String, userIds: NonEmptyList[DataSubject]): IO[Boolean] =
        (fr"select count(*) from data_subjects where appid = $appId and"
          ++ Fragments.in(fr"id", userIds))
          .query[Int]
          .unique
          .map(_ > 0)
          .transact(xa)

    }

}
