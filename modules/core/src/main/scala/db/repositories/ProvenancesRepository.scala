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

  def getProvenances(
      appId: UUID,
      userIds: List[DataSubject]
  ): IO[Map[DataCategory, List[Provenance]]]

  def getProvenanceForDataCategory(
      appId: UUID,
      dc: DataCategory
  ): IO[List[Provenance]]

}

// TODO: select for users
object ProvenancesRepository {
  def live(xa: Transactor[IO]): ProvenancesRepository =
    new ProvenancesRepository {

      def getProvenances(
          appId: UUID,
          userIds: List[DataSubject]
      ): IO[Map[DataCategory, List[Provenance]]] =
        sql"""
          select p.provenance, p.system, dc.term
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId
        """
          .query[(ProvenanceTerms, String, DataCategory)]
          .map {
            case (prov, system, dc) => dc -> Provenance(prov, system)
          }
          .to[List]
          .map(_.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def getProvenanceForDataCategory(
          appId: UUID,
          dc: DataCategory
      ): IO[List[Provenance]] =
        sql"""
          select p.provenance, p.system
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId and dc.term = $dc
        """
          .query[Provenance]
          .to[List]
          .transact(xa)

    }

}