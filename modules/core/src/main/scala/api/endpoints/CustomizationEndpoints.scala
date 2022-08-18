package io.blindnet.pce
package api.endpoints

import java.util.UUID

import cats.effect.IO
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
import api.endpoints.messages.customization.*
import io.blindnet.pce.priv.GeneralInformation
import io.blindnet.pce.priv.LegalBase

class CustomizationEndpoints(
    customizationService: CustomizationService
) {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  val base = baseEndpoint.in("customize").tag("Customization")

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")

  val getGeneralInfo =
    base
      .description("Get general information about the app")
      .get
      .in("general-info")
      .out(jsonBody[GeneralInformation])
      .serverLogicSuccess(_ => customizationService.getGeneralInfo(appId))

  val updateGeneralInfo =
    base
      .description("Update general information about the app")
      .put
      .in("general-info")
      .in(jsonBody[GeneralInformation])
      .serverLogicSuccess(req => customizationService.updateGeneralInfo(appId, req))

  val getPrivacyScopeDimensions =
    base
      .description("Get data categories, processing categories and purposes")
      .get
      .in("privacy-scope-dimensions")
      .out(jsonBody[PrivacyScopeDimensionsPayload])
      .serverLogicSuccess(req => customizationService.getPrivacyScopeDimensions(appId))

  // val getSelectors =
  //   base
  //     .description("Get the list of selectors defined in the app")
  //     .get
  //     .in("selectors")
  //     .out(jsonBody[List[SelectorInfoPayload]])
  //     .serverLogicSuccess(req => customizationService.getSelectors(appId))

  // val addSelector =
  //   base
  //     .description("Add new new selectors")
  //     .post
  //     .in("selectors")
  //     .in(jsonBody[SelectorInfoPayload])
  //     .serverLogicSuccess(req => ???)

  // add provenance/retention for data categories/selectors

  val getLegalBases =
    base
      .description("Get the list of legal bases")
      .get
      .in("legal-bases")
      .out(jsonBody[List[LegalBase]])
      .serverLogicSuccess(req => customizationService.getLegalBases(appId))

  val getLegalBase =
    base
      .description("Get a legal bases")
      .get
      .in("legal-bases")
      .in(path[UUID]("legalBaseId"))
      .out(jsonBody[LegalBase])
      .serverLogicSuccess(lbId => customizationService.getLegalBase(appId, lbId))

  val createLegalBase =
    base
      .description("Create new legal bases")
      .put
      .in("legal-bases")
      .in(jsonBody[CreateLegalBasePayload])
      .out(stringBody)
      .serverLogicSuccess(req => customizationService.createLegalBase(appId, req))

  val endpoints = List(
    getGeneralInfo,
    updateGeneralInfo,
    getPrivacyScopeDimensions,
    getLegalBases,
    getLegalBase,
    createLegalBase
  )

}
