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
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
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
            select type, subcat, name, description
            from legal_bases
            where appid = $appId::uuid
          """
          .query[(String, String, Option[String], Option[String])]
          .to[List]
          .map(
            _.flatMap {
              case (lbType, subcat, name, desc) =>
                LegalBaseTerms
                  .parse(subcat)
                  .toOption
                  .map(lbTerm => LegalBase(lbTerm, List.empty, name = name, description = desc))
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
            select type, subcat, name, description, dc, pc, pp
            from legal_bases
            where appid = $appId::uuid and id = $lbId::uuid
          """
          .query[
            (
                String,
                String,
                Option[String],
                Option[String],
                List[String],
                List[String],
                List[String]
            )
          ]
          .unique
          .map {
            case (lbType, subcat, name, desc, dcs, pcs, pps) => {
              for {
                lbTerm <- LegalBaseTerms.parse(subcat).toOption
                scope = dcs.lazyZip(pcs).lazyZip(pps).flatMap {
                  case (dc, pc, pp) => {
                    (
                      DataCategory.parse(dc),
                      ProcessingCategory.parse(pc),
                      Purpose.parse(pp)
                    ).mapN(PrivacyScopeTriple.apply).toOption
                  }
                }
              } yield LegalBase(lbTerm, scope, name = name, description = desc)
            }
          }
          .transact(xa)
      }

    }

}
