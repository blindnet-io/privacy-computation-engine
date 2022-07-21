package io.blindnet.privacy
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor

trait Repositories {
  val generalInfo: GeneralInfoRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
  val selector: SelectorRepository
}

object Repositories {
  def live(xa: Transactor[IO]): Repositories = {
    val generalInfoRepo  = GeneralInfoRepository.live(xa)
    val legalBaseRepo    = LegalBaseRepository.live(xa)
    val privacyScopeRepo = PrivacyScopeRepository.live(xa)
    val selectorRepo     = SelectorRepository.live(xa)

    new Repositories {
      val generalInfo  = generalInfoRepo
      val legalBase    = legalBaseRepo
      val privacyScope = privacyScopeRepo
      val selector     = selectorRepo
    }
  }

}
