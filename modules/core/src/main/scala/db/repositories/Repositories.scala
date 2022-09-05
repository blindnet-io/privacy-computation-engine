package io.blindnet.pce
package db.repositories

import cats.effect.*
import doobie.util.transactor.Transactor
import db.repositories.privacyrequest.*
import db.repositories.privacyscope.*
import db.repositories.events.*

trait Repositories {
  val app: AppRepository
  val generalInfo: GeneralInfoRepository
  val dataSubject: DataSubjectRepository
  val legalBase: LegalBaseRepository
  val privacyScope: PrivacyScopeRepository
  val retentionPolicy: RetentionPolicyRepository
  val provenance: ProvenancesRepository
  val privacyRequest: PrivacyRequestRepository
  val events: EventsRepository
  val regulations: RegulationsRepository

  val commands: CommandsRepository
  val demandsToReview: DemandsToReviewRepository

  val callbacks: CallbacksRepository
}

object Repositories {
  def live(xa: Transactor[IO], pools: Pools): IO[Repositories] = {

    val appRepo             = AppRepository.live(xa)
    val generalInfoRepo     = GeneralInfoRepository.live(xa)
    val dataSubjectRepo     = DataSubjectRepository.live(xa)
    val legalBaseRepo       = LegalBaseRepository.live(xa, pools)
    val privacyScopeRepo    = PrivacyScopeRepository.live(xa)
    val retentionPolicyRepo = RetentionPolicyRepository.live(xa)
    val provenanceRepo      = ProvenancesRepository.live(xa)
    val privacyReqRepo      = PrivacyRequestRepository.live(xa)
    val eventsRepo          = EventsRepository.live(xa)
    val regulationsRepo     = RegulationsRepository.live(xa)

    val commandsRepo        = CommandsRepository.live(xa)
    val demandsToReviewRepo = DemandsToReviewRepository.live(xa)

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
      val events          = eventsRepo
      val regulations     = regulationsRepo

      val commands        = commandsRepo
      val demandsToReview = demandsToReviewRepo

      val callbacks = callbacksRepo
    }
  }

}
