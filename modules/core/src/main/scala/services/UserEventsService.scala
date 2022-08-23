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
import io.blindnet.pce.model.DemandToRespond
import io.blindnet.pce.util.extension.*
import util.*
import priv.LegalBase
import priv.terms.EventTerms.*

class UserEventsService(
    repos: Repositories
) {
  // TODO: create user if not exists
  // TODO: repeating code, refactor

  def addConsentGivenEvent(appId: UUID, req: GiveConsentPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.consentId, false)
      isConsent = lbOpt.map(_.isConsent).getOrElse(false)
      _ <- isConsent.emptyNotFound(s"Consent ${req.consentId} not found")

      _ <- repos.events.addConsentGiven(req.consentId, req.dataSubject, req.date)
    } yield ()

  def addStartContractEvent(appId: UUID, req: StartContractPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.contractId, false)
      isContract = lbOpt.map(_.isContract).getOrElse(false)
      _ <- isContract.emptyNotFound(s"Contract ${req.contractId} not found")

      _ <- repos.events.addLegalBaseEvent(req.contractId, req.dataSubject, ServiceStart, req.date)
    } yield ()

  def addEndContractEvent(appId: UUID, req: EndContractPayload) =
    for {
      lbOpt <- repos.legalBase.get(appId, req.contractId, false)
      isContract = lbOpt.map(_.isContract).getOrElse(false)
      _ <- isContract.emptyNotFound(s"Contract ${req.contractId} not found")

      _ <- repos.events.addLegalBaseEvent(req.contractId, req.dataSubject, ServiceEnd, req.date)
    } yield ()

  def addStartLegitimateInterestEvent(appId: UUID, req: StartLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      lbOpt <- repos.legalBase.get(appId, id, false)
      isLegitimateInterest = lbOpt.map(_.isLegitimateInterest).getOrElse(false)
      _ <- isLegitimateInterest.emptyNotFound(s"Legitimate interest $id not found")

      _ <- repos.events.addLegalBaseEvent(id, req.dataSubject, ServiceStart, req.date)
    } yield ()

  def addEndLegitimateInterestEvent(appId: UUID, req: EndLegitimateInterestPayload) =
    val id = req.legitimateInterestId
    for {
      lbOpt <- repos.legalBase.get(appId, id, false)
      isLegitimateInterest = lbOpt.map(_.isLegitimateInterest).getOrElse(false)
      _ <- isLegitimateInterest.emptyNotFound(s"Legitimate interest $id not found")

      _ <- repos.events.addLegalBaseEvent(id, req.dataSubject, ServiceEnd, req.date)
    } yield ()

}
