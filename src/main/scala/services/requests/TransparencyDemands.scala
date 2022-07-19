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

class TransparencyDemands(
    giRepo: GeneralInfoRepository,
    psRepo: PrivacyScopeRepository,
    lbRepo: LegalBaseRepository
) {

  def processTransparency(appId: String, userIds: List[DataSubject]): IO[Unit] =
    IO.unit

  def getDataCategories(appId: String): IO[List[String]] =
    psRepo.getDataCategories(appId).map(_.map(_.term))

  def getDpo(appId: String): IO[List[Dpo]] =
    giRepo.getGeneralInfo(appId).map(_.dpo)

  def getOrganization(appId: String): IO[List[Organization]] =
    giRepo.getGeneralInfo(appId).map(_.organizations)

  def getPrivacyPolicy(appId: String): IO[String] =
    giRepo.getGeneralInfo(appId).map(_.privacyPolicyLink)

  def getUserKnown(appId: String, userIds: List[DataSubject]): IO[Boolean] =
    userIds match {
      case Nil => IO(false)
      case _   => giRepo.known(appId, userIds)
    }

  def getLebalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] =
    lbRepo.getLegalBases(appId, userIds)

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]] =
    psRepo.getProcessingCategories(appId, userIds)

  def getProvenances(appId: String, userIds: List[DataSubject]): IO[List[Provenance]] =
    psRepo.getProvenances(appId, userIds)

  def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]] =
    psRepo.getPurposes(appId, userIds)

  def getRetentions(
      appId: String,
      userIds: List[DataSubject]
  ): IO[Map[String, List[RetentionPolicy]]] =
    for {
      selectors         <- psRepo.getSelectors(appId, userIds)
      retentionPolicies <- psRepo.getRetentionPoliciesForSelector(appId, selectors.map(_.name))
    } yield retentionPolicies

  def getWhere(appId: String): IO[List[String]] =
    giRepo.getGeneralInfo(appId).map(_.countries)

  def getWho(appId: String): IO[List[String]] =
    giRepo.getGeneralInfo(appId).map(_.accessPolicies)

}
