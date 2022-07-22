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
    repositories: Repositories
) {

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val sRepo  = repositories.selector

  val notFoundErr = NotFoundException("Requested app could not be found")

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
