package io.blindnet.pce
package api.endpoints

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import io.blindnet.pce.priv.{ GeneralInformation, LegalBase }
import io.circe.generic.auto.*
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import services.*
import api.endpoints.BaseEndpoint.*
import api.endpoints.messages.configuration.*
import io.blindnet.pce.model.DemandResolutionStrategy

class ConfigurationEndpoints(
    configurationService: ConfigurationService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val Tag     = "Configuration"
  val DocsUri = "https://blindnet.dev/docs/computation/configuration"

  val base = baseEndpoint.in("configure").tag(Tag)

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")

  val getGeneralInfo =
    base
      .description("Get general information about the app")
      .get
      .in("general-info")
      .out(jsonBody[GeneralInformation])
      .serverLogicSuccess(_ => configurationService.getGeneralInfo(appId))

  val updateGeneralInfo =
    base
      .description("Update general information about the app")
      .put
      .in("general-info")
      .in(jsonBody[GeneralInformation])
      .serverLogicSuccess(req => configurationService.updateGeneralInfo(appId, req))

  val getDemandResolutionStrategy =
    base
      .description("Get information about demand resolution strategies")
      .get
      .in("demand-resolution-strategy")
      .out(jsonBody[DemandResolutionStrategy])
      .serverLogicSuccess(_ => configurationService.getDemandResolutionStrategy(appId))

  val updateAutomaticResolution =
    base
      .description("Update demand resolution strategies")
      .put
      .in("demand-resolution-strategy")
      .in(jsonBody[DemandResolutionStrategy])
      .serverLogicSuccess(req => configurationService.updateDemandResolutionStrategy(appId, req))

  val getPrivacyScopeDimensions =
    base
      .description("Get data categories, processing categories and purposes")
      .get
      .in("privacy-scope-dimensions")
      .out(jsonBody[PrivacyScopeDimensionsPayload])
      .serverLogicSuccess(req => configurationService.getPrivacyScopeDimensions(appId))

  val addSelectors =
    base
      .description("Add selectors")
      .put
      .in("selectors")
      .in(jsonBody[List[CreateSelectorPayload]])
      .serverLogicSuccess(req => configurationService.addSelectors(appId, req))

  val getLegalBases =
    base
      .description("Get the list of legal bases")
      .get
      .in("legal-bases")
      .out(jsonBody[List[LegalBase]])
      .serverLogicSuccess(req => configurationService.getLegalBases(appId))

  val getLegalBase =
    base
      .description("Get a legal bases")
      .get
      .in("legal-bases")
      .in(path[UUID]("legalBaseId"))
      .out(jsonBody[LegalBase])
      .serverLogicSuccess(lbId => configurationService.getLegalBase(appId, lbId))

  val createLegalBase =
    base
      .description("Create new legal bases")
      .put
      .in("legal-bases")
      .in(jsonBody[CreateLegalBasePayload])
      .out(stringBody)
      .serverLogicSuccess(req => configurationService.createLegalBase(appId, req))

  val addRetentionPolicies =
    base
      .description("Create retention policies for data categories")
      .put
      .in("retention-policies")
      .in(jsonBody[List[CreateRetentionPolicyPayload]])
      .serverLogicSuccess(req => configurationService.addRetentionPolicies(appId, req))

  val deleteRetentionPolicy =
    base
      .description("Delete retention policy")
      .delete
      .in("retention-policies")
      .in(path[UUID]("retentionPolicyId"))
      .serverLogicSuccess(id => configurationService.deleteRetentionPolicy(appId, id))

  val addProvenances =
    base
      .description("Create provenances for data categories")
      .put
      .in("provenances")
      .in(jsonBody[List[CreateProvenancePayload]])
      .serverLogicSuccess(req => configurationService.addProvenances(appId, req))

  val deleteProvenance =
    base
      .description("Delete provenance")
      .delete
      .in("provenances")
      .in(path[UUID]("provenanceId"))
      .serverLogicSuccess(id => configurationService.deleteProvenance(appId, id))

  def getDataCategories =
    base
      .description("Get data categories with retention policies and provenances")
      .get
      .in("data-categories")
      .out(jsonBody[List[DataCategoryResponsePayload]])
      .serverLogicSuccess(_ => configurationService.getDataCategories(appId))

  def getAllRegulations =
    base
      .description("Get all regulations")
      .get
      .in("regulations")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogicSuccess(_ => configurationService.getAllRegulations())

  def getAppRegulations =
    base
      .description("Get regulations applied to the users of the app")
      .get
      .in("regulations")
      .in("app")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogicSuccess(_ => configurationService.getAppRegulations(appId))

  val addRegulation =
    base
      .description("Assign regulation to an app")
      .put
      .in("regulations")
      .in(jsonBody[AddRegulationsPayload])
      .serverLogicSuccess(req => configurationService.addRegulations(appId, req))

  val deleteRegulation =
    base
      .description("Delete regulation assigned to an app")
      .delete
      .in("regulations")
      .in(path[UUID]("regulationId"))
      .serverLogicSuccess(id => configurationService.deleteRegulation(appId, id))

  val endpoints = List(
    getGeneralInfo,
    updateGeneralInfo,
    getPrivacyScopeDimensions,
    addSelectors,
    getLegalBases,
    getLegalBase,
    createLegalBase,
    addRetentionPolicies,
    deleteRetentionPolicy,
    addProvenances,
    deleteProvenance,
    getDataCategories,
    getAllRegulations,
    getAppRegulations,
    addRegulation,
    deleteRegulation,
    getDemandResolutionStrategy,
    updateAutomaticResolution
  )

}
