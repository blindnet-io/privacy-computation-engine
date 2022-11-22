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
import api.endpoints.messages.userevents.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.terms.*
import util.*
import priv.LegalBase
import priv.terms.EventTerms.*
import io.blindnet.pce.priv.PSContext
import io.blindnet.pce.priv.PrivacyScope

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

  def storeConsent(id: UUID, ds: DataSubject, t: Instant) =
    repos.events.addConsentGiven(id, ds, t)

  def addConsentGivenEvent(req: GiveConsentUnsafePayload) =
    for {
      _ <- verifyLbExists(req.appId, req.consentId, _.isConsent)
      ds = req.dataSubject.toPrivDataSubject(req.appId)
      _         <- handleUser(req.appId, ds)
      timestamp <- Clock[IO].realTimeInstant
      _         <- storeConsent(req.consentId, ds, timestamp)
    } yield ()

  def giveConsentProactive(jwt: UserJwt)(req: GiveConsentProactive) =
    for {
      ctx <- repos.privacyScope.getContext(jwt.appId)
      scope = req.getPrivPrivacyScope.zoomIn(ctx)
      // TODO
      _ <- "Scope too large".failBadRequest.whenA(scope.triples.size > 1000)
      _ <- "Bad privacy scope".failBadRequest.unlessA(PrivacyScope.validate(scope, ctx))
      ds = DataSubject(jwt.userId, jwt.appId)
      _         <- handleUser(jwt.appId, ds)
      timestamp <- Clock[IO].realTimeInstant
      lbOpt     <- repos.legalBase.getByScope(jwt.appId, scope)
      consentId <- lbOpt match {
        case None     =>
          for {
            id <- UUIDGen.randomUUID[IO]
            lb = LegalBase(id, LegalBaseTerms.Consent, scope, None, None, true)
            _ <- repos.legalBase.add(jwt.appId, lb, true)
            _ <- storeConsent(id, ds, timestamp)
            // TODO: handling error
            _ <- repos.legalBase.addScope(jwt.appId, lb.id, lb.scope).start
          } yield id
        case Some(id) => {
          for {
            _ <- storeConsent(id, ds, timestamp)
          } yield id
        }
      }
    } yield consentId.toString

  def addConsentGivenEvent(jwt: UserJwt)(req: GiveConsentPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.consentId, _.isConsent)
      ds = DataSubject(jwt.userId, jwt.appId)
      _         <- handleUser(jwt.appId, ds)
      timestamp <- Clock[IO].realTimeInstant
      _         <- storeConsent(req.consentId, ds, timestamp)
    } yield ()

  def storeGivenConsentEvent(jwt: AppJwt)(req: StoreGivenConsentPayload) =
    for {
      _ <- verifyLbExists(jwt.appId, req.consentId, _.isConsent)
      ds = req.dataSubject.toPrivDataSubject(jwt.appId)
      _ <- handleUser(jwt.appId, ds)
      _ <- storeConsent(req.consentId, ds, req.date)
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
