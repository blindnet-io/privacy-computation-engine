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
import io.circe.Encoder

class TransparencyDemands(
    repositories: Repositories
) {

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val rpRepo = repositories.retentionPolicy
  val prRepo = repositories.provenance

  extension [T](io: IO[Option[T]])
    def failIfNotFound =
      io.flatMap {
        case None    => IO.raiseError(NotFoundException("Requested app could not be found"))
        case Some(t) => IO(t)
      }

  extension [T: Encoder](io: IO[T])
    def json =
      io.map(_.asJson)

  def processTransparencyDemand(
      demand: Demand,
      appId: String,
      userIds: List[DataSubject],
      date: Instant
  ): IO[DemandResponse] = {
    for {
      answer <- demand.action match {
        case Action.Transparency          => processTransparency(appId, userIds).json
        case Action.TDataCategories       => psRepo.getDataCategories(appId).json
        case Action.TDPO                  => getDpo(appId).json
        case Action.TKnown                => getUserKnown(appId, userIds).json
        case Action.TLegalBases           => lbRepo.getLegalBases(appId, userIds).json
        case Action.TOrganization         => getOrganization(appId).json
        case Action.TPolicy               => getPrivacyPolicy(appId).json
        case Action.TProcessingCategories => psRepo.getProcessingCategories(appId, userIds).json
        case Action.TProvenance           => prRepo.getProvenances(appId, userIds).json
        case Action.TPurpose              => psRepo.getPurposes(appId, userIds).json
        case Action.TRetention            => rpRepo.getRetentionPolicies(appId, userIds).json
        case Action.TWhere                => getWhere(appId).json
        case Action.TWho                  => getWho(appId).json
        case _                            => IO.raiseError(new NotImplementedError)
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
      .failIfNotFound
      .map(_.dpo)

  def getOrganization(appId: String): IO[String] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.organization)

  def getPrivacyPolicy(appId: String): IO[Option[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.privacyPolicyLink)

  def getUserKnown(appId: String, userIds: List[DataSubject]): IO[Boolean] =
    NonEmptyList.fromList(userIds) match {
      case None          => IO(false)
      case Some(userIds) => giRepo.known(appId, userIds)
    }

  def getWhere(appId: String): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.countries)

  def getWho(appId: String): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.dataConsumerCategories)

}
