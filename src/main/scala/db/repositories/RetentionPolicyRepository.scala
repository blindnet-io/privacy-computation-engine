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
import javax.xml.crypto.Data

trait RetentionPolicyRepository {

  def getRetentionPolicies(
      appId: String,
      userIds: List[DataSubject]
  ): IO[Map[DataCategory, List[RetentionPolicy]]]

  def getRetentionPolicyForDataCategory(
      appId: String,
      dc: DataCategory
  ): IO[List[RetentionPolicy]]

}

// TODO: select for users
object RetentionPolicyRepository {
  def live(xa: Transactor[IO]): RetentionPolicyRepository =
    new RetentionPolicyRepository {

      def getRetentionPolicies(
          appId: String,
          userIds: List[DataSubject]
      ): IO[Map[DataCategory, List[RetentionPolicy]]] =
        sql"""
          select rp.policy, rp.duration, rp.after, dc.term
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId::uuid
        """
          .query[(String, String, String, String)]
          .to[List]
          .map(_.flatMap {
            case (policy, dur, after, dc) =>
              for {
                p <- RetentionPolicyTerms.parse(policy).toOption
                a <- EventTerms.parse(after).toOption
                d <- DataCategory.parse(dc).toOption
              } yield d -> RetentionPolicy(p, dur, a)
          }.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def getRetentionPolicyForDataCategory(
          appId: String,
          dc: DataCategory
      ): IO[List[RetentionPolicy]] =
        sql"""
          select rp.policy, rp.duration, rp.after
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId::uuid and dc.term = $dc
        """
          .query[(String, String, String)]
          .to[List]
          .map(_.flatMap {
            case (policy, dur, after) =>
              for {
                p <- RetentionPolicyTerms.parse(policy).toOption
                a <- EventTerms.parse(after).toOption
              } yield RetentionPolicy(p, dur, a)
          })
          .transact(xa)

    }

}
