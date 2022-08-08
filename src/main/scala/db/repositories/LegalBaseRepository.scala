package io.blindnet.privacy
package db.repositories

import java.util.UUID

import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import model.vocabulary.*
import model.vocabulary.terms.*

trait LegalBaseRepository {
  def getLegalBases(appId: UUID, userIds: List[DataSubject]): IO[List[LegalBase]]

  def getLegalBase(
      appId: UUID,
      lbId: String,
      userIds: List[DataSubject]
  ): IO[Option[LegalBase]]

}

object LegalBaseRepository {

  def live(xa: Transactor[IO]): LegalBaseRepository =
    new LegalBaseRepository {
      // TODO: userId
      def getLegalBases(appId: UUID, userIds: List[DataSubject]): IO[List[LegalBase]] = {
        sql"""
            select type, name, description, active
            from legal_bases
            where appid = $appId
          """
          .query[(LegalBaseTerms, Option[String], Option[String], Boolean)]
          .map {
            case (lbType, name, desc, active) =>
              LegalBase(lbType, PrivacyScope.empty, name, desc, active)
          }
          .to[List]
          .transact(xa)
      }

      // TODO: how to derive Get[List[T]] if T has Get instance?
      def getLegalBase(
          appId: UUID,
          lbId: String,
          userIds: List[DataSubject]
      ): IO[Option[LegalBase]] = {
        sql"""
          select lb.id as id, lb.appid as appid, lb.type as type, lb.name as name, lb.description as description, lb.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
          from legal_bases lb
            join legal_bases_scope lbs on lbs.lbid = lb.id
            join "scope" s on s.id = lbs.scid
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
          where lb.appid = $appId and lb.id = $lbId
          group by lb.id;
        """
          .query[
            (
                LegalBaseTerms,
                Option[String],
                Option[String],
                Boolean,
                List[String],
                List[String],
                List[String]
            )
          ]
          .map {
            case (lbTerm, name, desc, active, dcs, pcs, pps) => {
              val scope = dcs.lazyZip(pcs).lazyZip(pps).map {
                case (dc, pc, pp) => PrivacyScopeTriple.unsafe(dc, pc, pp)
              }
              LegalBase(lbTerm, PrivacyScope.empty, name, desc, active)
            }
          }
          .option
          .transact(xa)
      }

    }

}
