package io.blindnet.privacy
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor

trait Repositories {
  val generalInfo: GeneralInfoRepository
  val dataSubject: DataSubjectRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
  val retentionPolicy: RetentionPolicyRepository
  val provenance: ProvenancesRepository
  val privacyRequest: PrivacyRequestRepository

  val pendingRequests: PendingRequestsRepository
  val pendingDemands: PendingDemandsRepository
}

object Repositories {
  def live(xa: Transactor[IO]): IO[Repositories] = {
    val generalInfoRepo     = GeneralInfoRepository.live(xa)
    val dataSubjectRepo     = DataSubjectRepository.live(xa)
    val legalBaseRepo       = LegalBaseRepository.live(xa)
    val privacyScopeRepo    = PrivacyScopeRepository.live(xa)
    val retentionPolicyRepo = RetentionPolicyRepository.live(xa)
    val provenanceRepo      = ProvenancesRepository.live(xa)
    val privacyReqRepo      = PrivacyRequestRepository.live(xa)

    val pendingDemandsRepo = PendingDemandsRepository.live(xa)

    for {
      pendingReqsRepo <- PendingRequestsRepository.live()
    } yield new Repositories {
      val generalInfo     = generalInfoRepo
      val dataSubject     = dataSubjectRepo
      val legalBase       = legalBaseRepo
      val privacyScope    = privacyScopeRepo
      val retentionPolicy = retentionPolicyRepo
      val provenance      = provenanceRepo
      val privacyRequest  = privacyReqRepo

      val pendingRequests = pendingReqsRepo
      val pendingDemands  = pendingDemandsRepo
    }
  }

}
