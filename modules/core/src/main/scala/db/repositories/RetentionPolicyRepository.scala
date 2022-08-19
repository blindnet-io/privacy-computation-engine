package io.blindnet.pce
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
import priv.*
import priv.terms.*
import db.DbUtil

trait RetentionPolicyRepository {

  def get(appId: UUID): IO[Map[DataCategory, List[RetentionPolicy]]]

  def get(appId: UUID, dc: DataCategory): IO[List[RetentionPolicy]]

  def add(appId: UUID, rs: NonEmptyList[(DataCategory, RetentionPolicy)]): IO[Unit]

  def delete(appId: UUID, rpId: UUID): IO[Unit]

  def deleteAll(appId: UUID, dcId: UUID): IO[Unit]

}

// TODO: select for users
object RetentionPolicyRepository {
  def live(xa: Transactor[IO]): RetentionPolicyRepository =
    new RetentionPolicyRepository {

      def get(appId: UUID): IO[Map[DataCategory, List[RetentionPolicy]]] =
        sql"""
          select rp.id, rp.policy, rp.duration, rp.after, dc.term
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId
        """
          .query[(UUID, RetentionPolicyTerms, String, EventTerms, DataCategory)]
          .map {
            case (id, policy, dur, after, dc) => dc -> RetentionPolicy(id, policy, dur, after)
          }
          .to[List]
          .map(_.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def get(appId: UUID, dc: DataCategory): IO[List[RetentionPolicy]] =
        sql"""
          select rp.id, rp.policy, rp.duration, rp.after
          from retention_policies rp
          join data_categories dc ON rp.dcid = dc.id
          where rp.appid = $appId and dc.term = $dc
        """
          .query[RetentionPolicy]
          .to[List]
          .transact(xa)

      def add(appId: UUID, rs: NonEmptyList[(DataCategory, RetentionPolicy)]): IO[Unit] = {
        val sql = s"""
          insert into retention_policies (id, appid, dcid, policy, duration, after)
          values (?, '$appId', (select id from data_categories where term = ?), ?::policy_terms, ?, ?::event_terms)
        """
        Update[(UUID, DataCategory, String, String, String)](sql)
          .updateMany(
            rs.map(r => (r._2.id, r._1, r._2.policyType.encode, r._2.duration, r._2.after.encode))
          )
          .transact(xa)
          .void
      }

      def delete(appId: UUID, rpId: UUID): IO[Unit] =
        sql"""delete from retention_policies where appid = $appId and id = $rpId""".update.run
          .transact(xa)
          .void

      def deleteAll(appId: UUID, dcId: UUID): IO[Unit] =
        sql"""delete from retention_policies where appid = $appId and dcid = $dcId""".update.run
          .transact(xa)
          .void

    }

}
