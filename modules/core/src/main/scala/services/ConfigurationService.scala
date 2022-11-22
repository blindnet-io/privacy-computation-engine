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
import model.*
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.*
import priv.terms.*

class ConfigurationService(
    repos: Repositories
) {

  def getGeneralInfo(appId: UUID)(x: Unit) =
    repos.generalInfo
      .get(appId)
      .orNotFound(s"General info for app ${appId} not found")

  def updateGeneralInfo(appId: UUID)(gi: GeneralInformation) =
    repos.generalInfo
      .upsert(appId, gi)
      .handleErrorWith(_ => s"General info for app ${appId} not found".failNotFound)

  def getDemandResolutionStrategy(appId: UUID)(x: Unit) =
    repos.app
      .get(appId)
      .orNotFound(s"App ${appId} not found")
      .map(_.resolutionStrategy)

  def updateDemandResolutionStrategy(appId: UUID)(drs: DemandResolutionStrategy) =
    repos.app
      .updateReslutionStrategy(appId, drs)
      .handleErrorWith(_ => s"General info for app ${appId} not found".failNotFound)

  def getPrivacyScopeDimensions(appId: UUID)(x: Unit) =
    val dcs = DataCategory.getAllDataCategories
    val pcs = ProcessingCategory.getAllProcessingCategories
    val pps = Purpose.getAllPurposes
    IO(PrivacyScopeDimensionsPayload(dcs, pcs, pps))

  def addSelectors(appId: UUID)(req: List[CreateSelectorPayload]) =
    for {
      _ <- req.forall(_.dataCategory.term != "*").onFalseBadRequest("Selector can't be top level")
      dist = req.distinct
      reqNel <- NonEmptyList.fromList(dist).fold("Add at least one selector".failBadRequest)(IO(_))
      validated = reqNel.traverse(p => DataCategory.parseOnlyTerm(p.dataCategory.term).map(_ => p))
      s <- validated.toEither.left.map(_ => "Unknown data category").orBadRequest
      selectors = s.map(s => s.dataCategory.copy(term = s"${s.dataCategory.term}.${s.name}"))
      ids <- reqNel.traverse(_ => UUIDGen.randomUUID[IO])
      _   <- repos.privacyScope
        .addSelectors(appId, ids zip selectors)
        .handleErrorWith(_ => "Selector already exists".failBadRequest)

      ss = (s zip selectors).toList

      provenances <- repos.provenance.get(appId)
      sProv       <-
        ss.flatMap(s => provenances.get(s._1.dataCategory).getOrElse(List.empty).map(s._2 -> _))
          .traverse { case (dc, p) => UUIDGen.randomUUID[IO].map(id => (dc, p.copy(id = id))) }

      retPolicies <- repos.retentionPolicy.get(appId)
      sRPs        <-
        ss.flatMap(s => retPolicies.get(s._1.dataCategory).getOrElse(List.empty).map(s._2 -> _))
          .traverse { case (dc, rp) => UUIDGen.randomUUID[IO].map(id => (dc, rp.copy(id = id))) }

      _ <- sProv.toNel.fold(IO.unit)(l => repos.provenance.add(appId, l))
      _ <- sRPs.toNel.fold(IO.unit)(l => repos.retentionPolicy.add(appId, l))
    } yield ()

  def getLegalBases(appId: UUID)(x: Unit) =
    repos.legalBase.get(appId, scope = false)

  def getLegalBase(appId: UUID)(lbId: UUID) =
    repos.legalBase
      .get(appId, lbId, true)
      .orNotFound(s"Legal base with id $lbId not found")
      .map(lb => lb.copy(scope = lb.scope.zoomIn()))

  def createLegalBase(appId: UUID)(req: CreateLegalBasePayload) =
    for {
      id  <- UUIDGen.randomUUID[IO]
      ctx <- repos.privacyScope.getContext(appId)
      scope = req.getPrivPrivacyScope.zoomIn(ctx)
      _ <- s"Bad privacy scope".failBadRequest.unlessA(PrivacyScope.validate(scope, ctx))
      lb = LegalBase(id, req.lbType, scope, req.name, req.description, true)
      _ <- repos.legalBase.add(appId, lb)
      // TODO: handling error
      _ <- repos.legalBase.addScope(appId, lb.id, lb.scope).start
    } yield id.toString

  def addRetentionPolicies(appId: UUID)(req: List[CreateRetentionPolicyPayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one policy")
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <- reqNel
        .forall(r => DataCategory.exists(r.dataCategory, selectors))
        .onFalseBadRequest("Unknown data category")

      rps <- reqNel.flatTraverse {
        r =>
          DataCategory
            .granularizeKeepParents(r.dataCategory, selectors)
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

  def deleteRetentionPolicy(appId: UUID)(id: UUID) =
    repos.retentionPolicy.delete(appId, id)

  def addProvenances(appId: UUID)(req: List[CreateProvenancePayload]) =
    for {
      reqNel    <- req.toNel.orBadRequest("Add at least one provenance")
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      _         <- reqNel
        .forall(r => DataCategory.exists(r.dataCategory, selectors))
        .onFalseBadRequest("Unknown data category")

      ps <- reqNel.flatTraverse {
        r =>
          DataCategory
            .granularizeKeepParents(r.dataCategory, selectors)
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

  def deleteProvenance(appId: UUID)(id: UUID) =
    for {
      _ <- repos.provenance.delete(appId, id)
    } yield ()

  def getDataCategories(appId: UUID)(x: Unit) =
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

  def getAllRegulations(appId: UUID)(x: Unit) =
    repos.regulations.getInfo().map(_.map(RegulationResponsePayload.fromRegulationInfo))

  def getAppRegulations(appId: UUID)(x: Unit) =
    for {
      regs <- repos.regulations.getInfo(appId)
      resp = regs.map(RegulationResponsePayload.fromRegulationInfo)
    } yield resp

  def addRegulations(appId: UUID)(req: AddRegulationsPayload) =
    for {
      idsNel <- req.regulationIds.distinct.toNel.orBadRequest("Add at least one regulation")
      idsOk  <- repos.regulations.exists(idsNel).onFalseBadRequest("Unknown regulation id")
      _      <- repos.regulations.add(appId, idsNel)
    } yield ()

  def deleteRegulation(appId: UUID)(regId: UUID) =
    repos.regulations.delete(appId, regId)

}
