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
import io.blindnet.pce.services.storage.StorageInterface
import io.blindnet.pce.priv.GeneralInformation
import io.blindnet.pce.api.endpoints.messages.customization.PrivacyScopeDimensionsPayload

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

}
