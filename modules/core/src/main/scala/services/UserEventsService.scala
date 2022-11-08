package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.identityclient.auth.*
import io.blindnet.pce.api.endpoints.messages.consumerinterface.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.util.extension.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.terms.*
import util.*
import priv.LegalBase
import priv.terms.EventTerms.*

class UserEventsService(
    repos: Repositories
) {
  def verifyLbExists(appId: UUID, id: UUID, check: LegalBase => Boolean) =
    for {
      lbOpt <- repos.legalBase.get(appId, id, false)
      _     <- lbOpt.map(check).getOrElse(false).onFalseNotFound(s"Legal base $id not found")
    } yield ()

  def handleUser(appId: UUID, ds: DataSubject) =
    repos.dataSubject.exist(appId, ds.id).ifM(IO.unit, repos.dataSubject.insert(appId, ds))

  def addConsentGivenEvent(req: GiveConsentUnsafePayload) =
    for {
      _ <- verifyLbExists(req.appId, req.consentId, _.isConsent)
      ds = req.dataSubject.toPrivDataSubject(req.appId)
      _         <- handleUser(req.appId, ds)
      timestamp <- Clock[IO].realTimeInstant
      _         <- repos.events.addConsentGiven(req.consentId, ds, timestamp)
    } yield ()

  def addConsentGivenEvent(jwt: UserJwt)(req: GiveConsentPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.consentId, _.isConsent)
      ds = DataSubject(jwt.userId, jwt.appId)
      _         <- handleUser(jwt.appId, ds)
      timestamp <- Clock[IO].realTimeInstant
      _         <- repos.events.addConsentGiven(req.consentId, ds, timestamp)
    } yield ()

  def storeGivenConsentEvent(jwt: AppJwt)(req: StoreGivenConsentPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.consentId, _.isConsent)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- repos.events.addConsentGiven(req.consentId, ds, req.date)
    } yield ()

  def addStartContractEvent(jwt: AppJwt)(req: StartContractPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.contractId, _.isContract)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceStart, req.date)
    } yield ()

  def addEndContractEvent(jwt: AppJwt)(req: EndContractPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.contractId, _.isContract)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceEnd, req.date)
    } yield ()

  def addStartLegitimateInterestEvent(jwt: AppJwt)(req: StartLegitimateInterestPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.legitimateInterestId, _.isLegitimateInterest)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- repos.events.addLegalBaseEvent(req.legitimateInterestId, ds, ServiceStart, req.date)
    } yield ()

  def addEndLegitimateInterestEvent(jwt: AppJwt)(req: EndLegitimateInterestPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.legitimateInterestId, _.isLegitimateInterest)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- repos.events.addLegalBaseEvent(req.legitimateInterestId, ds, ServiceEnd, req.date)
    } yield ()

}
