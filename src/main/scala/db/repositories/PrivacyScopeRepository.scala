package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*
import db.DbUtil

trait PrivacyScopeRepository  {
  def getDataCategories(appId: String): IO[List[DataCategory]]

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]]

  def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]]
}

// TODO: select for users
object PrivacyScopeRepository {
  def live(xa: Transactor[IO]): PrivacyScopeRepository =
    new PrivacyScopeRepository {
      def getDataCategories(appId: String): IO[List[DataCategory]] =
        sql"""
          select distinct(dc.term) from legal_bases lb
          join legal_bases_scope lbsc on lbsc.lbid = lb.id
          join "scope" s on s.id = lbsc.scid
          join data_categories dc on dc.id = s.dcid
          where lb.active and dc.active and lb.appid = $appId::uuid
        """
          .query[DataCategory]
          .to[List]
          .transact(xa)

      def getProcessingCategories(
          appId: String,
          userIds: List[DataSubject]
      ): IO[List[ProcessingCategory]] =
        sql"""
          select distinct(pc.term) from legal_bases lb
          join legal_bases_scope lbsc on lbsc.lbid = lb.id
          join "scope" s on s.id = lbsc.scid
          join processing_categories pc on pc.id = s.pcid
          where lb.active and lb.appid = $appId::uuid
        """
          .query[ProcessingCategory]
          .to[List]
          .transact(xa)

      def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]] =
        sql"""
          select distinct(pp.term) from legal_bases lb
          join legal_bases_scope lbsc on lbsc.lbid = lb.id
          join "scope" s on s.id = lbsc.scid
          join processing_purposes pp on pp.id = s.ppid
          where lb.active and lb.appid = $appId::uuid
        """
          .query[Purpose]
          .to[List]
          .transact(xa)

    }

}
