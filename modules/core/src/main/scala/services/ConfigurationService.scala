package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.*

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.identityclient.auth.*
import io.blindnet.pce.api.endpoints.messages.configuration.*
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
import io.blindnet.pce.model.DemandResolutionStrategy

class ConfigurationService(
    repos: Repositories
) {

  def getGeneralInfo(jwt: AppJwt)(x: Unit) =
    repos.generalInfo
      .get(jwt.appId)
      .orFail(s"General info for app ${jwt.appId} not found")

  def updateGeneralInfo(jwt: AppJwt)(gi: GeneralInformation) =
    repos.generalInfo.upsert(jwt.appId, gi)

  def getDemandResolutionStrategy(jwt: AppJwt)(x: Unit) =
    repos.app
      .get(jwt.appId)
      .orFail(s"General info for app ${jwt.appId} not found")
      .map(_.resolutionStrategy)

  def updateDemandResolutionStrategy(jwt: AppJwt)(drs: DemandResolutionStrategy) =
    repos.app.updateReslutionStrategy(jwt.appId, drs)

  def getPrivacyScopeDimensions(jwt: AppJwt)(x: Unit) =
    for {
      dcs <- repos.privacyScope.getDataCategories(jwt.appId, withSelectors = false)
      pcs <- repos.privacyScope.getProcessingCategories(jwt.appId)
      pps <- repos.privacyScope.getPurposes(jwt.appId)
      resp = PrivacyScopeDimensionsPayload(dcs, pcs, pps)
    } yield resp

  def addSelectors(jwt: AppJwt)(req: List[CreateSelectorPayload]) =
    for {
      _ <- req.forall(_.dataCategory.term != "*").onFalseBadRequest("Selector can't be top level")
      reqNel <- NonEmptyList.fromList(req).fold("Add at least one selector".failBadRequest)(IO(_))
      ids    <- reqNel.traverse(_ => UUIDGen.randomUUID[IO])
      selectors = reqNel.map(p => p.dataCategory.copy(term = s"${p.dataCategory.term}.${p.name}"))
      _ <- repos.privacyScope.addSelectors(jwt.appId, ids zip selectors)
    } yield ()

  def getLegalBases(jwt: AppJwt)(x: Unit) =
    repos.legalBase.get(jwt.appId, scope = false)

  // TODO: granular privacy scope
  def getLegalBase(jwt: AppJwt)(lbId: UUID) =
    repos.legalBase
      .get(jwt.appId, lbId, true)
      .orNotFound(s"Legal base with id $lbId not found")

  def createLegalBase(jwt: AppJwt)(req: CreateLegalBasePayload) =
    for {
      id        <- UUIDGen.randomUUID[IO]
      selectors <- repos.privacyScope.getSelectors(jwt.appId, active = true)
      ctx   = PSContext(selectors)
      scope = req.getPrivPrivacyScope.zoomIn(ctx)
      lb    = LegalBase(id, req.lbType, scope, req.name, req.description, true)
      // TODO: handling error
      _ <- repos.legalBase.add(jwt.appId, lb).start
    } yield id.toString

  def addRetentionPolicies(jwt: AppJwt)(req: List[CreateRetentionPolicyPayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one policy")
      selectors <- repos.privacyScope.getSelectors(jwt.appId, active = true)
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
      _   <- repos.retentionPolicy.add(jwt.appId, rps)
    } yield ()

  def deleteRetentionPolicy(jwt: AppJwt)(id: UUID) =
    for {
      _ <- repos.retentionPolicy.delete(jwt.appId, id)
    } yield ()

  def addProvenances(jwt: AppJwt)(req: List[CreateProvenancePayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one provenance")
      selectors <- repos.privacyScope.getSelectors(jwt.appId, active = true)
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
      _  <- repos.provenance.add(jwt.appId, ps)
    } yield ()

  def deleteProvenance(jwt: AppJwt)(id: UUID) =
    for {
      _ <- repos.provenance.delete(jwt.appId, id)
    } yield ()

  def getDataCategories(jwt: AppJwt)(x: Unit) =
    for {
      dcs <- repos.privacyScope.getAllDataCategories(jwt.appId)
      ps  <- repos.provenance.get(jwt.appId)
      rps <- repos.retentionPolicy.get(jwt.appId)

      res = dcs.toList.map(
        dc => {
          val p  = ps.getOrElse(dc, List.empty)
          val rp = rps.getOrElse(dc, List.empty)
          DataCategoryResponsePayload(dc, p, rp)
        }
      )
    } yield res

  def getAllRegulations(jwt: AppJwt)(x: Unit) =
    repos.regulations.getInfo().map(_.map(RegulationResponsePayload.fromRegulationInfo))

  def getAppRegulations(jwt: AppJwt)(x: Unit) =
    for {
      regs <- repos.regulations.getInfo(jwt.appId)
      resp = regs.map(RegulationResponsePayload.fromRegulationInfo)
    } yield resp

  def addRegulations(jwt: AppJwt)(req: AddRegulationsPayload) =
    for {
      idsNel <- req.regulationIds.distinct.toNel.orBadRequest("Add at least one regulation")
      idsOk  <- repos.regulations.exists(idsNel).onFalseBadRequest("Unknown regulation id")
      _      <- repos.regulations.add(jwt.appId, idsNel)
    } yield ()

  def deleteRegulation(jwt: AppJwt)(regId: UUID) =
    for {
      _ <- repos.regulations.delete(jwt.appId, regId)
    } yield ()

}
