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
import io.blindnet.pce.api.endpoints.messages.configuration.*
import scala.concurrent.duration.*

class ConfigurationService(
    repos: Repositories
) {

  def getGeneralInfo(appId: UUID) =
    repos.generalInfo
      .get(appId)
      .orFail(s"General info for app $appId not found")

  def updateGeneralInfo(appId: UUID, gi: GeneralInformation) =
    repos.generalInfo.upsert(appId, gi)

  def getPrivacyScopeDimensions(appId: UUID) =
    for {
      dcs <- repos.privacyScope.getDataCategories(appId, selectors = false)
      pcs <- repos.privacyScope.getProcessingCategories(appId)
      pps <- repos.privacyScope.getPurposes(appId)
      resp = PrivacyScopeDimensionsPayload(dcs, pcs, pps)
    } yield resp

  def addSelectors(appId: UUID, req: List[CreateSelectorPayload]) =
    for {
      reqNel <- NonEmptyList.fromList(req).fold("Add at least one selector".failBadRequest)(IO(_))
      ids    <- UUIDGen.randomUUID[IO].replicateA(reqNel.length)
      idsNel = NonEmptyList.fromList(ids).get
      _ <-
        if req.exists(_.dataCategory.term == "*") then "Selector can't be top level".failBadRequest
        else IO.unit
      selectors = reqNel.map(p => p.dataCategory.copy(term = s"${p.dataCategory.term}.${p.name}"))
      _ <- repos.privacyScope.addSelectors(appId, idsNel zip selectors)
    } yield ()

  def getLegalBases(appId: UUID) =
    for {
      res <- repos.legalBase.get(appId, scope = false)
    } yield res

  def getLegalBase(appId: UUID, lbId: UUID) =
    for {
      res <- repos.legalBase
        .get(appId, lbId, true)
        .orNotFound(s"Legal base with id $lbId not found")
    } yield res

  def createLegalBase(appId: UUID, req: CreateLegalBasePayload) =
    for {
      id        <- UUIDGen.randomUUID[IO]
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      scope = req.getPrivPrivacyScope.zoomIn(selectors)
      lb    = LegalBase(id, req.lbType, scope, req.name, req.description, true)
      // TODO: handling error
      _ <- repos.legalBase.add(appId, lb).start
    } yield id.toString

  def addRetentionPolicies(appId: UUID, req: List[CreateRetentionPolicyPayload]) =
    for {
      _         <- if req.length == 0 then "Add at least one policy".failBadRequest else IO.unit
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <-
        if !req.forall(r => DataCategory.exists(r.dataCategory, selectors))
        then "Unknown data category".failBadRequest
        else IO.unit

      ids <- UUIDGen.randomUUID[IO].replicateA(req.length)

      rps <- req.flatTraverse {
        r =>
          DataCategory
            .getMostGranular(r.dataCategory, selectors)
            .toList
            .traverse(
              dc =>
                for {
                  id <- UUIDGen.randomUUID[IO]
                  res = (dc, RetentionPolicy(id, r.policy, r.duration, r.after))
                } yield res
            )
      }
      rpsNel = NonEmptyList.fromListUnsafe(rps)
      _   <- repos.retentionPolicy.add(appId, rpsNel)
    } yield ()

  def deleteRetentionPolicy(appId: UUID, id: UUID) =
    for {
      _ <- repos.retentionPolicy.delete(appId, id)
    } yield ()

  def addProvenances(appId: UUID, req: List[CreateProvenancePayload]) =
    for {
      _         <- if req.length == 0 then "Add at least one provenance".failBadRequest else IO.unit
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <-
        if !req.forall(r => DataCategory.exists(r.dataCategory, selectors))
        then "Unknown data category".failBadRequest
        else IO.unit

      ids <- UUIDGen.randomUUID[IO].replicateA(req.length)

      ps <- req.flatTraverse {
        r =>
          DataCategory
            .getMostGranular(r.dataCategory, selectors)
            .toList
            .traverse(
              dc =>
                for {
                  id <- UUIDGen.randomUUID[IO]
                  res = (dc, Provenance(id, r.provenance, r.system))
                } yield res
            )
      }
      psNel = NonEmptyList.fromListUnsafe(ps)
      _  <- repos.provenance.add(appId, psNel)
    } yield ()

  def deleteProvenance(appId: UUID, id: UUID) =
    for {
      _ <- repos.provenance.delete(appId, id)
    } yield ()

  def getDataCategories(appId: UUID) =
    for {
      dcs <- repos.privacyScope.getAllDataCategories(appId)
      ps  <- repos.provenance.get(appId)
      rps <- repos.retentionPolicy.get(appId)

      res = dcs.toList.map(
        dc => {
          val p  = ps.getOrElse(dc, List.empty)
          val rp = rps.getOrElse(dc, List.empty)
          DataCategoryResponsePayload(dc, p, rp)
        }
      )
    } yield res

}
