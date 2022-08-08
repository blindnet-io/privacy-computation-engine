package io.blindnet.privacy
package tasks

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.implicits.*
import io.blindnet.privacy.util.extension.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{ Encoder, Json }
import db.repositories.*
import model.vocabulary.request.PrivacyRequest
import model.vocabulary.request.Demand
import model.error.*
import model.vocabulary.*
import model.vocabulary.terms.*

class TransparencyDemands(
    repositories: Repositories
) {

  import Action.*

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val rpRepo = repositories.retentionPolicy
  val prRepo = repositories.provenance
  val dsRepo = repositories.dataSubject

  extension [T](io: IO[Option[T]])
    def failIfNotFound = io.orNotFound("Requested app could not be found")

  extension [T: Encoder](io: IO[T])
    def json =
      io.map(_.asJson)

  def getAnswer(
      demand: Demand,
      appId: UUID,
      userIds: List[DataSubject]
  ): IO[Json] = {
    demand.action match {
      case Transparency          => processTransparency(appId, userIds).json
      case TDataCategories       => psRepo.getDataCategories(appId).json
      case TDPO                  => getDpo(appId).json
      case TKnown                => getUserKnown(appId, userIds).json
      case TLegalBases           => lbRepo.getLegalBases(appId, userIds).json
      case TOrganization         => getOrganization(appId).json
      case TPolicy               => getPrivacyPolicy(appId).json
      case TProcessingCategories => psRepo.getProcessingCategories(appId, userIds).json
      case TProvenance           => prRepo.getProvenances(appId, userIds).json
      case TPurpose              => psRepo.getPurposes(appId, userIds).json
      case TRetention            => rpRepo.getRetentionPolicies(appId, userIds).json
      case TWhere                => getWhere(appId).json
      case TWho                  => getWho(appId).json
      case _                     => IO.raiseError(new NotImplementedError)
    }
  }

  private def processTransparency(
      appId: UUID,
      userIds: List[DataSubject]
  ): IO[Map[Action, Json]] = {
    val all = List(
      psRepo.getDataCategories(appId).json.map((TDataCategories, _)),
      getDpo(appId).json.map((TDPO, _)),
      getUserKnown(appId, userIds).json.map((TKnown, _)),
      lbRepo.getLegalBases(appId, userIds).json.map((TLegalBases, _)),
      getOrganization(appId).json.map((TOrganization, _)),
      getPrivacyPolicy(appId).json.map((TPolicy, _)),
      psRepo.getProcessingCategories(appId, userIds).json.map((TProcessingCategories, _)),
      prRepo.getProvenances(appId, userIds).json.map((TProvenance, _)),
      psRepo.getPurposes(appId, userIds).json.map((TPurpose, _)),
      rpRepo.getRetentionPolicies(appId, userIds).json.map((TRetention, _)),
      getWhere(appId).json.map((TWhere, _)),
      getWho(appId).json.map((TWho, _))
    ).parSequence

    all.map(_.toMap)
  }

  private def getDpo(appId: UUID): IO[String] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.dpo)

  private def getOrganization(appId: UUID): IO[String] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.organization)

  private def getPrivacyPolicy(appId: UUID): IO[Option[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.privacyPolicyLink)

  private def getUserKnown(appId: UUID, userIds: List[DataSubject]): IO[BooleanTerms] =
    NonEmptyList.fromList(userIds) match {
      case None          => IO(BooleanTerms.No)
      case Some(userIds) =>
        dsRepo.known(appId, userIds).map(r => if r then BooleanTerms.Yes else BooleanTerms.No)
    }

  private def getWhere(appId: UUID): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.countries)

  private def getWho(appId: UUID): IO[List[String]] =
    giRepo
      .getGeneralInfo(appId)
      .failIfNotFound
      .map(_.dataConsumerCategories)

}
