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

trait ProvenancesRepository {

  def get(appId: UUID): IO[Map[DataCategory, List[Provenance]]]

  def get(appId: UUID, dc: DataCategory): IO[List[Provenance]]

  def add(appId: UUID, rs: NonEmptyList[(DataCategory, Provenance)]): IO[Unit]

  def delete(appId: UUID, pId: UUID): IO[Unit]

  def deleteAll(appId: UUID, dcId: UUID): IO[Unit]
}

// TODO: select for users
object ProvenancesRepository {
  def live(xa: Transactor[IO]): ProvenancesRepository =
    new ProvenancesRepository {

      def get(appId: UUID): IO[Map[DataCategory, List[Provenance]]] =
        sql"""
          select p.id, p.provenance, p.system, dc.term
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId
        """
          .query[(UUID, ProvenanceTerms, Option[String], DataCategory)]
          .map {
            case (id, prov, system, dc) => dc -> Provenance(id, prov, system)
          }
          .to[List]
          .map(_.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def get(appId: UUID, dc: DataCategory): IO[List[Provenance]] =
        sql"""
          select p.id, p.provenance, p.system
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId and dc.term = $dc
        """
          .query[Provenance]
          .to[List]
          .transact(xa)

      def add(appId: UUID, rs: NonEmptyList[(DataCategory, Provenance)]): IO[Unit] = {
        val sql = s"""
          insert into provenances (id, appid, dcid, provenance, system)
          values (?, '$appId', (select id from data_categories where term = ?), ?::provenance_terms, ?)
        """
        Update[(UUID, DataCategory, String, Option[String])](sql)
          .updateMany(
            rs.map(r => (r._2.id, r._1, r._2.provenance.encode, r._2.system))
          )
          .transact(xa)
          .void
      }

      def delete(appId: UUID, pId: UUID): IO[Unit] =
        sql"""delete from provenances where appid = $appId and id = $pId""".update.run
          .transact(xa)
          .void

      def deleteAll(appId: UUID, dcId: UUID): IO[Unit] =
        sql"""delete from provenances where appid = $appId and dcid = $dcId""".update.run
          .transact(xa)
          .void

    }

}
