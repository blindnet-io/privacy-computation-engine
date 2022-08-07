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
import model.vocabulary.terms.*
import db.DbUtil
import javax.xml.crypto.Data

trait ProvenancesRepository {

  def getProvenances(
      appId: String,
      userIds: List[DataSubject]
  ): IO[Map[DataCategory, List[Provenance]]]

  def getProvenanceForDataCategory(
      appId: String,
      dc: DataCategory
  ): IO[List[Provenance]]

}

// TODO: select for users
object ProvenancesRepository {
  def live(xa: Transactor[IO]): ProvenancesRepository =
    new ProvenancesRepository {

      def getProvenances(
          appId: String,
          userIds: List[DataSubject]
      ): IO[Map[DataCategory, List[Provenance]]] =
        sql"""
          select p.provenance, p.system, dc.term
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId::uuid
        """
          .query[(ProvenanceTerms, String, DataCategory)]
          .map {
            case (prov, system, dc) => dc -> Provenance(prov, system)
          }
          .to[List]
          .map(_.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

      def getProvenanceForDataCategory(
          appId: String,
          dc: DataCategory
      ): IO[List[Provenance]] =
        sql"""
          select p.provenance, p.system
          from provenances p
          join data_categories dc ON p.dcid = dc.id
          where p.appid = $appId::uuid and dc.term = $dc
        """
          .query[Provenance]
          .to[List]
          .transact(xa)

    }

}
