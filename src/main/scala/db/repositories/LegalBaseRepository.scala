package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait LegalBaseRepository[F[_]] {
  def getLegalBases(appId: String, userIds: List[DataSubject]): F[List[LegalBase]]
}
