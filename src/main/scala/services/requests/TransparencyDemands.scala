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
import api.endpoints.payload.*
import java.time.Instant

class TransparencyDemands(
    repositories: Repositories
) {

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val rpRepo = repositories.retentionPolicy
  val prRepo = repositories.provenance

  val notFoundErr = NotFoundException("Requested app could not be found")

  def processTransparencyDemand(
      demand: Demand,
      appId: String,
      userIds: List[DataSubject],
      date: Instant
  ): IO[DemandResponse] = {
    for {
      answer <- demand.action match {
        case Action.Transparency    => processTransparency(appId, userIds).map(_.asJson)
        case Action.TDataCategories => psRepo.getDataCategories(appId).map(_.map(_.term).asJson)
        case Action.TDPO            => getDpo(appId).map(_.asJson)
        case Action.TKnown          => getUserKnown(appId, userIds).map(_.asJson)
        // TODO user
        case Action.TLegalBases     => lbRepo.getLegalBases(appId, userIds).map(_.asJson)
        case Action.TOrganization   => getOrganization(appId).map(_.asJson)
        case Action.TPolicy         => getPrivacyPolicy(appId).map(_.asJson)
        case Action.TProcessingCategories =>
          psRepo.getProcessingCategories(appId, userIds).map(_.map(_.term).asJson) // TODO user
        case Action.TProvenance => prRepo.getProvenances(appId, userIds).map(_.asJson) // TODO user
        case Action.TPurpose    =>
          psRepo.getPurposes(appId, userIds).map(_.map(_.term).asJson) // TODO user
        case Action.TRetention  =>
          rpRepo.getRetentionPolicies(appId, userIds).map(_.asJson) // TODO user
        case Action.TWhere      => getWhere(appId).map(_.asJson)
        case Action.TWho        => getWho(appId).map(_.asJson)
        case _                  => IO.raiseError(new NotImplementedError)
      }
    } yield {
      DemandResponse(
        demand.id,
        demand.refId,
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

  def getDpo(appId: String): IO[String] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.dpo)))

  def getOrganization(appId: String): IO[String] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.organization)))

  def getPrivacyPolicy(appId: String): IO[Option[String]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.privacyPolicyLink)))

  def getUserKnown(appId: String, userIds: List[DataSubject]): IO[Boolean] =
    NonEmptyList.fromList(userIds) match {
      case None          => IO(false)
      case Some(userIds) => giRepo.known(appId, userIds)
    }

  def getWhere(appId: String): IO[List[String]] =
    giRepo.getGeneralInfo(appId).flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.countries)))

  def getWho(appId: String): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .flatMap(_.fold(IO.raiseError(notFoundErr))(x => IO(x.dataConsumerCategories)))

}
