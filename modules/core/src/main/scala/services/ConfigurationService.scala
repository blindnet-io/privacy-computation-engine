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
      dcs <- repos.privacyScope.getDataCategories(appId, withSelectors = false)
      pcs <- repos.privacyScope.getProcessingCategories(appId)
      pps <- repos.privacyScope.getPurposes(appId)
      resp = PrivacyScopeDimensionsPayload(dcs, pcs, pps)
    } yield resp

  def addSelectors(appId: UUID, req: List[CreateSelectorPayload]) =
    for {
      _ <- req.forall(_.dataCategory.term != "*").onFalseBadRequest("Selector can't be top level")
      reqNel <- NonEmptyList.fromList(req).fold("Add at least one selector".failBadRequest)(IO(_))
      ids    <- reqNel.traverse(_ => UUIDGen.randomUUID[IO])
      selectors = reqNel.map(p => p.dataCategory.copy(term = s"${p.dataCategory.term}.${p.name}"))
      _ <- repos.privacyScope.addSelectors(appId, ids zip selectors)
    } yield ()

  def getLegalBases(appId: UUID) =
    repos.legalBase.get(appId, scope = false)

  // TODO: granular privacy scope
  def getLegalBase(appId: UUID, lbId: UUID) =
    repos.legalBase
      .get(appId, lbId, true)
      .orNotFound(s"Legal base with id $lbId not found")

  def createLegalBase(appId: UUID, req: CreateLegalBasePayload) =
    for {
      id        <- UUIDGen.randomUUID[IO]
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      ctx   = PSContext(selectors)
      scope = req.getPrivPrivacyScope.zoomIn(ctx)
      lb    = LegalBase(id, req.lbType, scope, req.name, req.description, true)
      // TODO: handling error
      _ <- repos.legalBase.add(appId, lb).start
    } yield id.toString

  def addRetentionPolicies(appId: UUID, req: List[CreateRetentionPolicyPayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one policy")
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <- reqNel
        .forall(r => DataCategory.exists(r.dataCategory, selectors))
        .onFalseBadRequest("Unknown data category")

      rps <- reqNel.flatTraverse {
        r =>
          DataCategory
            .granularize(r.dataCategory, selectors)
            .toList
            .toNel
            .get
            .traverse(
              dc =>
                for {
                  id <- UUIDGen.randomUUID[IO]
                  res = (dc, RetentionPolicy(id, r.policy, r.duration, r.after))
                } yield res
            )
      }
      _   <- repos.retentionPolicy.add(appId, rps)
    } yield ()

  def deleteRetentionPolicy(appId: UUID, id: UUID) =
    for {
      _ <- repos.retentionPolicy.delete(appId, id)
    } yield ()

  def addProvenances(appId: UUID, req: List[CreateProvenancePayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one provenance")
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <- reqNel
        .forall(r => DataCategory.exists(r.dataCategory, selectors))
        .onFalseBadRequest("Unknown data category")

      ps <- reqNel.flatTraverse {
        r =>
          DataCategory
            .granularize(r.dataCategory, selectors)
            .toList
            .toNel
            .get
            .traverse(
              dc =>
                for {
                  id <- UUIDGen.randomUUID[IO]
                  res = (dc, Provenance(id, r.provenance, r.system))
                } yield res
            )
      }
      _  <- repos.provenance.add(appId, ps)
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

  def getAllRegulations() =
    repos.regulations.getInfo().map(_.map(RegulationResponsePayload.fromRegulationInfo))

  def getAppRegulations(appId: UUID) =
    for {
      regs <- repos.regulations.getInfo(appId)
      resp = regs.map(RegulationResponsePayload.fromRegulationInfo)
    } yield resp

  def addRegulations(appId: UUID, req: AddRegulationsPayload) =
    for {
      idsNel <- req.regulationIds.distinct.toNel.orBadRequest("Add at least one regulation")
      idsOk  <- repos.regulations.exists(idsNel).onFalseBadRequest("Unknown regulation id")
      _      <- repos.regulations.add(appId, idsNel)
    } yield ()

  def deleteRegulation(appId: UUID, regId: UUID) =
    for {
      _ <- repos.regulations.delete(appId, regId)
    } yield ()

}
