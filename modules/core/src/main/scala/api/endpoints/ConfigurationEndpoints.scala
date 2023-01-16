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
import api.endpoints.messages.configuration.*
import io.blindnet.identityclient.auth.*
import io.blindnet.pce.model.*
import io.blindnet.pce.priv.terms.DataCategory
import io.blindnet.pce.api.endpoints.messages.administration.CreateApplicationStoragePayload

class ConfigurationEndpoints(
    jwtAuthenticator: JwtAuthenticator[Jwt],
    identityAuthenticator: JwtLocalAuthenticator[AppJwt],
    configurationService: ConfigurationService
) {
  import util.*

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  lazy val Tag = "Configuration"
  val DocsUri  = "https://blindnet.dev/docs/computation/configuration"

  val base = baseEndpoint.in("configure").tag(Tag)

  val authenticatedEndpoint =
    jwtAuthenticator
      .mapJwt(_.appId)
      .or(identityAuthenticator.mapJwt(_.appId))
      .secureEndpoint(base)

  val getGeneralInfo =
    authenticatedEndpoint
      .description("Get general information about the app")
      .get
      .in("general-info")
      .out(jsonBody[GeneralInformation])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getGeneralInfo))

  val updateGeneralInfo =
    authenticatedEndpoint
      .description("Update general information about the app")
      .put
      .in("general-info")
      .in(jsonBody[GeneralInformation])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.updateGeneralInfo))

  val getDemandResolutionStrategy =
    authenticatedEndpoint
      .description("Get information about demand resolution strategies")
      .get
      .in("demand-resolution-strategy")
      .out(jsonBody[DemandResolutionStrategy])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getDemandResolutionStrategy))

  val updateAutomaticResolution =
    authenticatedEndpoint
      .description("Update demand resolution strategies")
      .put
      .in("demand-resolution-strategy")
      .in(jsonBody[DemandResolutionStrategy])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.updateDemandResolutionStrategy))

  val getPrivacyScopeDimensions =
    authenticatedEndpoint
      .description("Get data categories, processing categories and purposes")
      .get
      .in("privacy-scope-dimensions")
      .out(jsonBody[PrivacyScopeDimensionsPayload])
      .serverLogic(runLogicSuccess(configurationService.getPrivacyScopeDimensions))

  val getSelectors =
    authenticatedEndpoint
      .description("Get selectors")
      .get
      .in("selectors")
      .out(jsonBody[List[DataCategory]])
      .serverLogic(runLogicSuccess(configurationService.getSelectors))

  val addSelectors =
    authenticatedEndpoint
      .description("Add selectors")
      .put
      .in("selectors")
      .in(jsonBody[List[CreateSelectorPayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addSelectors))

  val getLegalBases =
    authenticatedEndpoint
      .description("Get the list of legal bases")
      .get
      .in("legal-bases")
      .out(jsonBody[List[LegalBase]])
      .serverLogic(runLogicSuccess(configurationService.getLegalBases))

  val getLegalBase =
    authenticatedEndpoint
      .description("Get a legal bases")
      .get
      .in("legal-bases")
      .in(path[UUID]("legalBaseId"))
      .out(jsonBody[LegalBase])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getLegalBase))

  val createLegalBase =
    authenticatedEndpoint
      .description("Create new legal bases")
      .put
      .in("legal-bases")
      .in(jsonBody[CreateLegalBasePayload])
      .out(stringBody)
      .errorOutVariant(unprocessable)
      .serverLogic(runLogic(configurationService.createLegalBase))

  val addRetentionPolicies =
    authenticatedEndpoint
      .description("Create retention policies for data categories")
      .put
      .in("retention-policies")
      .in(jsonBody[List[CreateRetentionPolicyPayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addRetentionPolicies))

  val deleteRetentionPolicy =
    authenticatedEndpoint
      .description("Delete retention policy")
      .delete
      .in("retention-policies")
      .in(path[UUID]("retentionPolicyId"))
      .serverLogic(runLogicSuccess(configurationService.deleteRetentionPolicy))

  val addProvenances =
    authenticatedEndpoint
      .description("Create provenances for data categories")
      .put
      .in("provenances")
      .in(jsonBody[List[CreateProvenancePayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addProvenances))

  val deleteProvenance =
    authenticatedEndpoint
      .description("Delete provenance")
      .delete
      .in("provenances")
      .in(path[UUID]("provenanceId"))
      .serverLogic(runLogicSuccess(configurationService.deleteProvenance))

  def getDataCategories =
    authenticatedEndpoint
      .description("Get data categories with retention policies and provenances")
      .get
      .in("data-categories")
      .out(jsonBody[List[DataCategoryResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getDataCategories))

  def getAllRegulations =
    authenticatedEndpoint
      .description("Get all regulations")
      .get
      .in("regulations")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getAllRegulations))

  def getAppRegulations =
    authenticatedEndpoint
      .description("Get regulations applied to the users of the app")
      .get
      .in("regulations")
      .in("app")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getAppRegulations))

  val addRegulation =
    authenticatedEndpoint
      .description("Assign regulation to an app")
      .put
      .in("regulations")
      .in(jsonBody[AddRegulationsPayload])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addRegulations))

  val deleteRegulation =
    authenticatedEndpoint
      .description("Delete regulation assigned to an app")
      .delete
      .in("regulations")
      .in(path[UUID]("regulationId"))
      .serverLogic(runLogicSuccess(configurationService.deleteRegulation))

  val getStorageConfiguration =
    authenticatedEndpoint
      .description("Create app storage")
      .get
      .in("storage")
      .out(jsonBody[StorageConfigurationPayload])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getStorageConfiguration))

  val createStorage =
    authenticatedEndpoint
      .description("Create app storage")
      .put
      .in("storage")
      .in(jsonBody[CreateApplicationStoragePayload])
      .errorOutVariant(unprocessable)
      .serverLogic(runLogic(configurationService.createStorage))

  val endpoints = List(
    getGeneralInfo,
    updateGeneralInfo,
    getPrivacyScopeDimensions,
    getSelectors,
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
    updateAutomaticResolution,
    getStorageConfiguration,
    createStorage
  )

}
