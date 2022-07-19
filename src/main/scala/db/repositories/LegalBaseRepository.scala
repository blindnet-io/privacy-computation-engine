package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import doobie.util.transactor.Transactor
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
      def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] = ???
    }

}
