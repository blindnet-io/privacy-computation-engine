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
import io.blindnet.pce.model.DemandResolutionStrategy
import io.blindnet.pce.db.repositories.DashboardToken

class ConfigurationEndpoints(
    jwtAuthenticator: JwtAuthenticator[Jwt],
    stAuthenticator: StAuthenticator[DashboardToken, DashboardToken],
    configurationService: ConfigurationService
) extends Endpoints(jwtAuthenticator) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  lazy val Tag = "Configuration"
  val DocsUri  = "https://blindnet.dev/docs/computation/configuration"

  override def mapEndpoint(endpoint: EndpointT): EndpointT =
    endpoint.in("configure").tag(Tag)

  // TODO: create authenticator composer in identity-client lib
  val authEndpoint =
    mapEndpoint(baseEndpoint)
      .securityIn(header[Option[String]]("Authorization"))
      .errorOut(statusCode(StatusCode.Unauthorized).and(jsonBody[String].mapTo[AuthException]))
      .serverSecurityLogic(
        t =>
          jwtAuthenticator
            .mapJwt(_.appId)
            .authenticateHeader(t)
            .flatMap {
              case Left(_)      => stAuthenticator.mapSt(_.appId).authenticateHeader(t)
              case x @ Right(_) => IO.pure(x)
            }
            .map(_.left.map(_ => AuthException("Invalid token")))
      )

  val getGeneralInfo =
    authEndpoint
      .description("Get general information about the app")
      .get
      .in("general-info")
      .out(jsonBody[GeneralInformation])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getGeneralInfo))

  val updateGeneralInfo =
    authEndpoint
      .description("Update general information about the app")
      .put
      .in("general-info")
      .in(jsonBody[GeneralInformation])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.updateGeneralInfo))

  val getDemandResolutionStrategy =
    authEndpoint
      .description("Get information about demand resolution strategies")
      .get
      .in("demand-resolution-strategy")
      .out(jsonBody[DemandResolutionStrategy])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getDemandResolutionStrategy))

  val updateAutomaticResolution =
    authEndpoint
      .description("Update demand resolution strategies")
      .put
      .in("demand-resolution-strategy")
      .in(jsonBody[DemandResolutionStrategy])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.updateDemandResolutionStrategy))

  val getPrivacyScopeDimensions =
    authEndpoint
      .description("Get data categories, processing categories and purposes")
      .get
      .in("privacy-scope-dimensions")
      .out(jsonBody[PrivacyScopeDimensionsPayload])
      .serverLogic(runLogicSuccess(configurationService.getPrivacyScopeDimensions))

  val addSelectors =
    authEndpoint
      .description("Add selectors")
      .put
      .in("selectors")
      .in(jsonBody[List[CreateSelectorPayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addSelectors))

  val getLegalBases =
    authEndpoint
      .description("Get the list of legal bases")
      .get
      .in("legal-bases")
      .out(jsonBody[List[LegalBase]])
      .serverLogic(runLogicSuccess(configurationService.getLegalBases))

  val getLegalBase =
    authEndpoint
      .description("Get a legal bases")
      .get
      .in("legal-bases")
      .in(path[UUID]("legalBaseId"))
      .out(jsonBody[LegalBase])
      .errorOutVariants(notFound)
      .serverLogic(runLogic(configurationService.getLegalBase))

  val createLegalBase =
    authEndpoint
      .description("Create new legal bases")
      .put
      .in("legal-bases")
      .in(jsonBody[CreateLegalBasePayload])
      .out(stringBody)
      .serverLogic(runLogicSuccess(configurationService.createLegalBase))

  val addRetentionPolicies =
    authEndpoint
      .description("Create retention policies for data categories")
      .put
      .in("retention-policies")
      .in(jsonBody[List[CreateRetentionPolicyPayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addRetentionPolicies))

  val deleteRetentionPolicy =
    authEndpoint
      .description("Delete retention policy")
      .delete
      .in("retention-policies")
      .in(path[UUID]("retentionPolicyId"))
      .serverLogic(runLogicSuccess(configurationService.deleteRetentionPolicy))

  val addProvenances =
    authEndpoint
      .description("Create provenances for data categories")
      .put
      .in("provenances")
      .in(jsonBody[List[CreateProvenancePayload]])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addProvenances))

  val deleteProvenance =
    authEndpoint
      .description("Delete provenance")
      .delete
      .in("provenances")
      .in(path[UUID]("provenanceId"))
      .serverLogic(runLogicSuccess(configurationService.deleteProvenance))

  def getDataCategories =
    authEndpoint
      .description("Get data categories with retention policies and provenances")
      .get
      .in("data-categories")
      .out(jsonBody[List[DataCategoryResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getDataCategories))

  def getAllRegulations =
    authEndpoint
      .description("Get all regulations")
      .get
      .in("regulations")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getAllRegulations))

  def getAppRegulations =
    authEndpoint
      .description("Get regulations applied to the users of the app")
      .get
      .in("regulations")
      .in("app")
      .out(jsonBody[List[RegulationResponsePayload]])
      .serverLogic(runLogicSuccess(configurationService.getAppRegulations))

  val addRegulation =
    authEndpoint
      .description("Assign regulation to an app")
      .put
      .in("regulations")
      .in(jsonBody[AddRegulationsPayload])
      .errorOutVariants(unprocessable)
      .serverLogic(runLogic(configurationService.addRegulations))

  val deleteRegulation =
    authEndpoint
      .description("Delete regulation assigned to an app")
      .delete
      .in("regulations")
      .in(path[UUID]("regulationId"))
      .serverLogic(runLogicSuccess(configurationService.deleteRegulation))

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
