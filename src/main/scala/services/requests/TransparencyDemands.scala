package io.blindnet.privacy
package services.requests

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import db.repositories.*
import model.vocabulary.request.PrivacyRequest
import model.vocabulary.request.Demand
import model.error.*
import model.vocabulary.*
import model.vocabulary.terms.*
import model.vocabulary.general.*
import api.endpoints.payload.*
import java.time.Instant

class TransparencyDemands(
    repositories: Repositories
) {

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val sRepo  = repositories.selector

  val notFoundErr = NotFoundException("Requested app could not be found")

  def processTransparencyDemand(
      demand: Demand,
      appId: String,
      userIds: List[DataSubject],
      id: String,
      date: Instant
  ) = {
    for {
      answer <- demand.action match {
        case Action.Transparency          => processTransparency(appId, userIds).map(_.asJson)
        case Action.TDataCategories       => getDataCategories(appId).map(_.map(_.term).asJson)
        case Action.TDPO                  => getDpo(appId).map(_.asJson)
        case Action.TKnown                => getUserKnown(appId, userIds).map(_.asJson)
        // TODO user
        case Action.TLegalBases           => getLegalBases(appId, userIds).map(_.asJson)
        case Action.TOrganization         => getOrganization(appId).map(_.asJson)
        case Action.TPolicy               => getPrivacyPolicy(appId).map(_.asJson)
        case Action.TProcessingCategories =>
          getProcessingCategories(appId, userIds).map(_.map(_.term).asJson) // TODO user
        case Action.TProvenance           =>
          getProvenances(appId, userIds).map(_.asJson) // TODO user
        case Action.TPurpose              =>
          getPurposes(appId, userIds).map(_.map(_.term).asJson) // TODO user
        case Action.TRetention            =>
          getRetentions(appId, userIds)
            .map(_.map {
              case (s, rp) =>
                Json.obj(
                  "selector_id"        -> s.id.asJson,
                  "selector_name"      -> s.name.asJson,
                  "retention_policies" -> rp.asJson
                )
            }.asJson) // TODO user
        case Action.TWhere                => getWhere(appId).map(_.asJson)
        case Action.TWho                  => getWho(appId).map(_.asJson)
        case _                            => IO.raiseError(new NotImplementedError)
      }
    } yield {
      DemandResponse(
        id,
        demand.id,
        date,
        demand.action,
        Status.Granted,
        answer,
        None,
        lang = "en",
        None,
        None
      )
    }
  }

  def processTransparency(appId: String, userIds: List[DataSubject]): IO[Unit] =
    IO.unit

  def getDataCategories(appId: String): IO[List[DataCategory]] =
    psRepo.getDataCategories(appId)

  def getDpo(appId: String): IO[List[Dpo]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.dpo)))

  def getOrganization(appId: String): IO[List[Organization]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.organizations)))

  def getPrivacyPolicy(appId: String): IO[Option[String]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.privacyPolicyLink)))

  def getUserKnown(appId: String, userIds: List[DataSubject]): IO[Boolean] =
    NonEmptyList.fromList(userIds) match {
      case None          => IO(false)
      case Some(userIds) => giRepo.known(appId, userIds)
    }

  def getLegalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] =
    lbRepo.getLegalBases(appId, userIds)

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]] =
    psRepo.getProcessingCategories(appId, userIds)

  def getProvenances(appId: String, userIds: List[DataSubject]): IO[List[Selector]] =
    sRepo.getSelectors(appId, userIds)

  def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]] =
    psRepo.getPurposes(appId, userIds)

  def getRetentions(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[(Selector, List[RetentionPolicy])]] =
    for {
      selectors         <- sRepo.getSelectors(appId, userIds)
      retentionPolicies <- NonEmptyList.fromList(selectors) match {
        case None            => IO.pure(Map.empty)
        case Some(selectors) => sRepo.getRetentionPoliciesForSelector(appId, selectors.map(_.id))
      }
    } yield retentionPolicies.toList.flatMap {
      case (sId, rp) =>
        selectors.find(_.id == sId).map(s => (s, rp))
    }

  def getWhere(appId: String): IO[List[String]] =
    giRepo.getGeneralInfo(appId).flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.countries)))

  def getWho(appId: String): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.accessPolicies)))

}
