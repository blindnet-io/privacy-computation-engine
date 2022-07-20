package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import cats.implicits.*
import doobie.util.transactor.Transactor
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait LegalBaseRepository {
  def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]]
}

object LegalBaseRepository {
  def live(xa: Transactor[IO]): LegalBaseRepository =
    new LegalBaseRepository {
      // TODO: userId
      def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] = {
        sql"""
            select type, subcat, name, dc, pc, pp
            from legal_bases
            where appid = $appId
          """
          .query[(String, String, Option[String], List[String], List[String], List[String])]
          .to[List]
          .map(
            _.flatMap {
              case (lbType, subcat, name, dcs, pcs, pps) => {
                for {
                  lbTerm <- LegalBaseTerms.parse(subcat)
                  scope = dcs.lazyZip(pcs).lazyZip(pps).flatMap {
                    case (dc, pc, pp) =>
                      (
                        DataCategory.parse(dc),
                        ProcessingCategory.parse(pc),
                        Purpose.parse(pp)
                      ).mapN(PrivacyScopeTriple.apply)
                  }
                } yield LegalBase(lbTerm, scope, name = name)
              }
            }
          )
          .transact(xa)
      }

    }

}
