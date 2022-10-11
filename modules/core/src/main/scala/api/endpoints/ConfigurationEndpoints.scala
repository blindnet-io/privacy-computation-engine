package io.blindnet.pce
package api.endpoints

import java.util.UUID
import cats.data.NonEmptyList
import cats.effect.IO
import io.blindnet.pce.priv.{GeneralInformation, LegalBase}
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
import io.blindnet.identityclient.auth.*
import io.blindnet.pce.model.DemandResolutionStrategy

class ConfigurationEndpoints(
    authenticator: JwtAuthenticator[Jwt],
    configurationService: ConfigurationService
) extends Endpoints(authenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val Tag     = "Configuration"
  val DocsUri = "https://blindnet.dev/docs/computation/configuration"

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("configure").tag(Tag)

  val getGeneralInfo =
    appAuthEndpoint
      .description("Get general information about the app")
      .get
      .in("general-info")
      .out(jsonBody[GeneralInformation])
      .serverLogicSuccess(configurationService.getGeneralInfo)

  val updateGeneralInfo =
    appAuthEndpoint
      .description("Update general information about the app")
      .put
      .in("general-info")
      .in(jsonBody[GeneralInformation])
      .serverLogicSuccess(configurationService.updateGeneralInfo)

  val getDemandResolutionStrategy =
    appAuthEndpoint
      .description("Get information about demand resolution strategies")
      .get
      .in("demand-resolution-strategy")
      .out(jsonBody[DemandResolutionStrategy])
      .serverLogicSuccess(configurationService.getDemandResolutionStrategy)

  val updateAutomaticResolution =
    appAuthEndpoint
      .description("Update demand resolution strategies")
      .put
      .in("demand-resolution-strategy")
      .in(jsonBody[DemandResolutionStrategy])
      .serverLogicSuccess(configurationService.updateDemandResolutionStrategy)

  val getPrivacyScopeDimensions =
    appAuthEndpoint
      .description("Get data categories, processing categories and purposes")
      .get
      .in("privacy-scope-dimensions")
      .out(jsonBody[PrivacyScopeDimensionsPayload])
      .serverLogicSuccess(configurationService.getPrivacyScopeDimensions)

  val addSelectors =
    appAuthEndpoint
      .description("Add selectors")
      .put
      .in("selectors")
      .in(jsonBody[List[CreateSelectorPayload]])
      .serverLogicSuccess(configurationService.addSelectors)

  val getLegalBases =
    appAuthEndpoint
      .description("Get the list of legal bases")
      .get
      .in("legal-bases")
      .out(jsonBody[List[LegalBase]])
      .serverLogicSuccess(configurationService.getLegalBases)

  val getLegalBase =
    appAuthEndpoint
      .description("Get a legal bases")
      .get
      .in("legal-bases")
      .in(path[UUID]("legalBaseId"))
      .out(jsonBody[LegalBase])
      .serverLogicSuccess(configurationService.getLegalBase)

  val createLegalBase =
    appAuthEndpoint
      .description("Create new legal bases")
      .put
      .in("legal-bases")
      .in(jsonBody[CreateLegalBasePayload])
      .out(stringBody)
      .serverLogicSuccess(configurationService.createLegalBase)

  val addRetentionPolicies =
    appAuthEndpoint
      .description("Create retention policies for data categories")
      .put
      .in("retention-policies")
      .in(jsonBody[List[CreateRetentionPolicyPayload]])
      .serverLogicSuccess(configurationService.addRetentionPolicies)

  val deleteRetentionPolicy =
    appAuthEndpoint
      .description("Delete retention policy")
      .delete
      .in("retention-policies")
      .in(path[UUID]("retentionPolicyId"))
      .serverLogicSuccess(configurationService.deleteRetentionPolicy)

  val addProvenances =
    appAuthEndpoint
      .description("Create provenances for data categories")
      .put
      .in("provenances")
      .in(jsonBody[List[CreateProvenancePayload]])
      .serverLogicSuccess(configurationService.addProvenances)

  val deleteProvenance =
    appAuthEndpoint
      .description("Delete provenance")
      .delete
      .in("provenances")
      .in(path[UUID]("provenanceId"))
      .serverLogicSuccess(configurationService.deleteProvenance)

  def getDataCategories =
    appAuthEndpoint
      .description("Get data categories with retention policies and provenances")
      .get
      .in("data-categories")
      .out(jsonBody[List[DataCategoryResponsePayload]])
      .serverLogicSuccess(configurationService.getDataCategories)

  def getAllRegulations =
    appAuthEndpoint
      .description("Get all regulations")
      .get
      .in("regulations")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogicSuccess(configurationService.getAllRegulations)

  def getAppRegulations =
    appAuthEndpoint
      .description("Get regulations applied to the users of the app")
      .get
      .in("regulations")
      .in("app")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogicSuccess(configurationService.getAppRegulations)

  val addRegulation =
    appAuthEndpoint
      .description("Assign regulation to an app")
      .put
      .in("regulations")
      .in(jsonBody[AddRegulationsPayload])
      .serverLogicSuccess(configurationService.addRegulations)

  val deleteRegulation =
    appAuthEndpoint
      .description("Delete regulation assigned to an app")
      .delete
      .in("regulations")
      .in(path[UUID]("regulationId"))
      .serverLogicSuccess(configurationService.deleteRegulation)

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
