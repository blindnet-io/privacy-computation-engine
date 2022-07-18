package io.blindnet.privacy
package services.requests

import db.repositories.*
import model.vocabulary.request.PrivacyRequest
import model.vocabulary.request.Demand
import cats.data.*
import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*

import model.error.*
import model.vocabulary.*
import model.vocabulary.terms.*
import model.vocabulary.general.*

import io.circe.Json
import io.circe.generic.auto.*, io.circe.syntax.*

class TransparencyDemands(
    repo: Repository[IO]
) {

  def processTransparency(appId: String, userIds: List[DataSubject]): IO[Unit] =
    IO.unit

  def getDataCategories(appId: String): IO[List[String]] =
    repo.getDataCategories(appId).map(_.map(_.term))

  def getDpo(appId: String): IO[List[Dpo]] =
    repo.getGeneralInfo(appId).map(_.dpo)

  def getOrganization(appId: String): IO[List[Organization]] =
    repo.getGeneralInfo(appId).map(_.organizations)

  def getPrivacyPolicy(appId: String): IO[String] =
    repo.getGeneralInfo(appId).map(_.privacyPolicyLink)

  def getUserKnown(appId: String, userIds: List[DataSubject]): IO[Boolean] =
    userIds match {
      case Nil => IO(false)
      case _   => repo.known(appId, userIds)
    }

  def getLebalBases(appId: String, userIds: List[DataSubject]): IO[List[LegalBase]] =
    repo.getLegalBases(appId, userIds)

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]] =
    repo.getProcessingCategories(appId, userIds)

  def getProvenances(appId: String, userIds: List[DataSubject]): IO[List[Provenance]] =
    repo.getProvenances(appId, userIds)

  def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]] =
    repo.getPurposes(appId, userIds)

  def getRetentions(
      appId: String,
      userIds: List[DataSubject]
  ): IO[Map[String, List[RetentionPolicy]]] =
    for {
      selectors         <- repo.getSelectors(appId, userIds)
      retentionPolicies <- repo.getRetentionPoliciesForSelector(appId, selectors.map(_.name))
    } yield retentionPolicies

  def getWhere(appId: String): IO[List[String]] =
    repo.getGeneralInfo(appId).map(_.countries)

  def getWho(appId: String): IO[List[String]] =
    repo.getGeneralInfo(appId).map(_.accessPolicies)

}
