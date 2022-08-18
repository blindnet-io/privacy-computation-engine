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
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.*
import priv.terms.*
import io.blindnet.pce.api.endpoints.messages.customization.*
import scala.concurrent.duration.*

class CustomizationService(
    repos: Repositories
) {

  def getGeneralInfo(appId: UUID) =
    repos.generalInfo
      .get(appId)
      .orFail(s"General info for app $appId not found")

  def updateGeneralInfo(appId: UUID, gi: GeneralInformation) =
    repos.generalInfo.update(appId, gi)

  def getPrivacyScopeDimensions(appId: UUID) =
    for {
      dcs <- repos.privacyScope.getDataCategories(appId, selectors = false)
      pcs <- repos.privacyScope.getProcessingCategories(appId)
      pps <- repos.privacyScope.getPurposes(appId)
      resp = PrivacyScopeDimensionsPayload(dcs, pcs, pps)
    } yield resp

  def getLegalBases(appId: UUID) =
    for {
      res <- repos.legalBase.getLegalBases(appId, scope = false)
    } yield res

  def getLegalBase(appId: UUID, lbId: UUID) =
    for {
      res <- repos.legalBase
        .getLegalBase(appId, lbId)
        .orNotFound(s"Legal base with id $lbId not found")
    } yield res

  def createLegalBase(appId: UUID, req: CreateLegalBasePayload) =
    for {
      id        <- UUIDGen.randomUUID[IO]
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      triples = req.scope.flatMap(
        triple =>
          for {
            dc <- DataCategory.getSubTerms(triple.dc, selectors)
            pc <- ProcessingCategory.getSubTerms(triple.pc)
            pp <- Purpose.getSubTerms(triple.pp)
          } yield PrivacyScopeTriple(dc, pc, pp)
      )
      scope   = PrivacyScope(triples)
      lb      = LegalBase(id, req.lbType, scope, req.name, req.description, true)
      // TODO: handling error
      _ <- repos.legalBase.store(appId, lb).start
    } yield id.toString

}
