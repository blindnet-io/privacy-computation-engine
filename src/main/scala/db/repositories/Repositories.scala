package io.blindnet.privacy
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor

trait Repositories {
  val generalInfo: GeneralInfoRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
}

object Repositories {
  def live(xa: Transactor[IO]): Repositories = {
    val generalInfo  = GeneralInfoRepository.live(xa)
    val legalBase    = LegalBaseRepository.live(xa)
    val privacyScope = PrivacyScopeRepository.live(xa)

    new Repositories {
      val generalInfo  = generalInfo
      val legalBase    = legalBase
      val privacyScope = privacyScope
    }
  }

}
