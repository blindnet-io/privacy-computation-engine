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
import io.blindnet.pce.services.util.*
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
import io.blindnet.pce.util.extension.*
import util.*
import priv.LegalBase
import priv.terms.EventTerms.*

class UserEventsService(
    repos: Repositories
) {
  // TODO: repeating code, refactor

  def handleUser(appId: UUID, ds: DataSubject) =
    for {
      userExists <- repos.dataSubject.exist(appId, ds.id)
      _          <- if userExists then IO.unit else repos.dataSubject.insert(appId, ds)
    } yield ()

  def addConsentGivenEvent(appId: UUID, req: GiveConsentPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.consentId, false)
      isConsent = lbOpt.map(_.isConsent).getOrElse(false)
      _ <- isConsent.onFalseNotFound(s"Consent ${req.consentId} not found")
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addConsentGiven(req.consentId, ds, req.date)
    } yield ()

  def addStartContractEvent(appId: UUID, req: StartContractPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.contractId, false)
      isContract = lbOpt.map(_.isContract).getOrElse(false)
      _ <- isContract.onFalseNotFound(s"Contract ${req.contractId} not found")
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceStart, req.date)
    } yield ()

  def addEndContractEvent(appId: UUID, req: EndContractPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.contractId, false)
      isContract = lbOpt.map(_.isContract).getOrElse(false)
      _ <- isContract.onFalseNotFound(s"Contract ${req.contractId} not found")
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(req.contractId, ds, ServiceEnd, req.date)
    } yield ()

  def addStartLegitimateInterestEvent(appId: UUID, req: StartLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      lbOpt <- repos.legalBase.get(appId, id, false)
      isLegitimateInterest = lbOpt.map(_.isLegitimateInterest).getOrElse(false)
      _ <- isLegitimateInterest.onFalseNotFound(s"Legitimate interest $id not found")
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(id, ds, ServiceStart, req.date)
    } yield ()

  def addEndLegitimateInterestEvent(appId: UUID, req: EndLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      lbOpt <- repos.legalBase.get(appId, id, false)
      isLegitimateInterest = lbOpt.map(_.isLegitimateInterest).getOrElse(false)
      _ <- isLegitimateInterest.onFalseNotFound(s"Legitimate interest $id not found")
      ds = req.dataSubject.toPrivDataSubject(appId)
      _ <- handleUser(appId, ds)

      _ <- repos.events.addLegalBaseEvent(id, ds, ServiceEnd, req.date)
    } yield ()

}
