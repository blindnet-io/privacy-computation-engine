package io.blindnet.privacy
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor

trait Repositories {
  val generalInfo: GeneralInfoRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
  val retentionPolicy: RetentionPolicyRepository
  val provenance: ProvenancesRepository
}

object Repositories {
  def live(xa: Transactor[IO]): Repositories = {
    val generalInfoRepo     = GeneralInfoRepository.live(xa)
    val legalBaseRepo       = LegalBaseRepository.live(xa)
    val privacyScopeRepo    = PrivacyScopeRepository.live(xa)
    val retentionPolicyRepo = RetentionPolicyRepository.live(xa)
    val provenanceRepo      = ProvenancesRepository.live(xa)
    new Repositories {
      val generalInfo     = generalInfoRepo
      val legalBase       = legalBaseRepo
      val privacyScope    = privacyScopeRepo
      val retentionPolicy = retentionPolicyRepo
      val provenance      = provenanceRepo
    }
  }

}
