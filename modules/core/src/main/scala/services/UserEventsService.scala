package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
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
      isConsent = lbOpt.map(check).getOrElse(false)
      _ <- isConsent.onFalseNotFound(s"Legal base $id not found")
    } yield ()

  def handleUser(appId: UUID, ds: DataSubject) =
    repos.dataSubject.exist(appId, ds.id).ifM(IO.unit, repos.dataSubject.insert(appId, ds))

  def addConsentGivenEvent(appId: UUID, req: GiveConsentPayload) =
    for {
      _ <- verifyLbExists(appId, req.consentId, _.isConsent)
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addConsentGiven(req.consentId, ds, req.date)
    } yield ()

  def addStartContractEvent(appId: UUID, req: StartContractPayload) =
    for {
      _ <- verifyLbExists(appId, req.contractId, _.isContract)
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceStart, req.date)
    } yield ()

  def addEndContractEvent(appId: UUID, req: EndContractPayload) =
    for {
      _ <- verifyLbExists(appId, req.contractId, _.isContract)
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceEnd, req.date)
    } yield ()

  def addStartLegitimateInterestEvent(appId: UUID, req: StartLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      _ <- verifyLbExists(appId, req.legitimateInterestId, _.isLegitimateInterest)
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(id, ds, ServiceStart, req.date)
    } yield ()

  def addEndLegitimateInterestEvent(appId: UUID, req: EndLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      _ <- verifyLbExists(appId, req.legitimateInterestId, _.isLegitimateInterest)
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(id, ds, ServiceEnd, req.date)
    } yield ()

}
