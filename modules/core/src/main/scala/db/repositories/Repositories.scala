package io.blindnet.pce
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor
import db.repositories.privacyrequest.*

trait Repositories {
  val app: AppRepository
  val generalInfo: GeneralInfoRepository
  val dataSubject: DataSubjectRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
  val retentionPolicy: RetentionPolicyRepository
  val provenance: ProvenancesRepository
  val privacyRequest: PrivacyRequestRepository

  val demandsToProcess: DemandsToProcessRepository
  val demandsToReview: DemandsToReviewRepository
  val demandsToRespond: DemandsToRespondRepository

  val callbacks: CallbacksRepository
}

object Repositories {
  def live(xa: Transactor[IO]): IO[Repositories] = {

    val appRepo             = AppRepository.live(xa)
    val generalInfoRepo     = GeneralInfoRepository.live(xa)
    val dataSubjectRepo     = DataSubjectRepository.live(xa)
    val legalBaseRepo       = LegalBaseRepository.live(xa)
    val privacyScopeRepo    = PrivacyScopeRepository.live(xa)
    val retentionPolicyRepo = RetentionPolicyRepository.live(xa)
    val provenanceRepo      = ProvenancesRepository.live(xa)
    val privacyReqRepo      = PrivacyRequestRepository.live(xa)

    val demandsToProcessRepo = DemandsToProcessRepository.live(xa)
    val demandsToReviewRepo  = DemandsToReviewRepository.live(xa)
    val demandsToRespondRepo = DemandsToRespondRepository.live(xa)

    for {
      callbacksRepo <- CallbacksRepository.live()
    } yield new Repositories {
      val app             = appRepo
      val generalInfo     = generalInfoRepo
      val dataSubject     = dataSubjectRepo
      val legalBase       = legalBaseRepo
      val privacyScope    = privacyScopeRepo
      val retentionPolicy = retentionPolicyRepo
      val provenance      = provenanceRepo
      val privacyRequest  = privacyReqRepo

      val demandsToProcess = demandsToProcessRepo
      val demandsToReview  = demandsToReviewRepo
      val demandsToRespond = demandsToRespondRepo

      val callbacks = callbacksRepo
    }
  }

}
