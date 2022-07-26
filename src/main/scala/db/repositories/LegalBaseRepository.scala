package io.blindnet.privacy
package db.repositories

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
  def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]]

  def getLegalBase(
      appId: String,
      lbId: String,
      userIds: List[DataSubject]
  ): IO[Option[LegalBase]]

}

object LegalBaseRepository {
  def live(xa: Transactor[IO]): LegalBaseRepository =
    new LegalBaseRepository {
      // TODO: userId
      def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] = {
        sql"""
            select type, name, description, active
            from legal_bases
            where appid = $appId::uuid
          """
          .query[(String, Option[String], Option[String], Boolean)]
          .to[List]
          .map(
            _.flatMap {
              case (lbType, name, desc, active) =>
                LegalBaseTerms
                  .parse(lbType)
                  .toOption
                  .map(
                    lbTerm => LegalBase(lbTerm, List.empty, name = name, description = desc, active)
                  )
            }
          )
          .transact(xa)
      }

      def getLegalBase(
          appId: String,
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
          where lb.appid = $appId::uuid and lb.id = $lbId::uuid
          group by lb.id;
        """
          .query[
            (
                String,
                Option[String],
                Option[String],
                Boolean,
                List[String],
                List[String],
                List[String]
            )
          ]
          .unique
          .map {
            case (lbType, name, desc, active, dcs, pcs, pps) => {
              for {
                lbTerm <- LegalBaseTerms.parse(lbType).toOption
                scope = dcs.lazyZip(pcs).lazyZip(pps).flatMap {
                  case (dc, pc, pp) => {
                    (
                      DataCategory.parse(dc),
                      ProcessingCategory.parse(pc),
                      Purpose.parse(pp)
                    ).mapN(PrivacyScopeTriple.apply).toOption
                  }
                }
              } yield LegalBase(lbTerm, scope, name = name, description = desc, active)
            }
          }
          .transact(xa)
      }

    }

}
