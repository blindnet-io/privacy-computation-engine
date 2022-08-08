package io.blindnet.privacy
package db.repositories

import java.util.UUID
import javax.xml.crypto.Data

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.terms.*
import db.DbUtil

trait RetentionPolicyRepository {

  def getRetentionPolicies(
      appId: UUID,
      userIds: List[DataSubject]
  ): IO[Map[DataCategory, List[RetentionPolicy]]]

  def getRetentionPolicyForDataCategory(
      appId: UUID,
      dc: DataCategory
  ): IO[List[RetentionPolicy]]

}

// TODO: select for users
object RetentionPolicyRepository {
  def live(xa: Transactor[IO]): RetentionPolicyRepository =
    new RetentionPolicyRepository {

      def getRetentionPolicies(
          appId: UUID,
          userIds: List[DataSubject]
      ): IO[Map[DataCategory, List[RetentionPolicy]]] =
        sql"""
          select rp.policy, rp.duration, rp.after, dc.term
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId
        """
          .query[(RetentionPolicyTerms, String, EventTerms, DataCategory)]
          .map {
            case (policy, dur, after, dc) => dc -> RetentionPolicy(policy, dur, after)
          }
          .to[List]
          .map(_.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def getRetentionPolicyForDataCategory(
          appId: UUID,
          dc: DataCategory
      ): IO[List[RetentionPolicy]] =
        sql"""
          select rp.policy, rp.duration, rp.after
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId and dc.term = $dc
        """
          .query[RetentionPolicy]
          .to[List]
          .transact(xa)

    }

}
