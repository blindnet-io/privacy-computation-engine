package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait PrivacyScopeRepository[F[_]] {
  def getSelectors(appId: String, userIds: List[DataSubject]): F[List[Selector]]

  def addSelectors(appId: String, selectors: List[Selector]): F[Unit]

  def deleteSelectors(appId: String, selectorIds: List[String]): F[Unit]

  def getSelectorPrivacyScope(
      appId: String,
      selectors: List[String]
  ): F[Map[String, List[PrivacyScope]]]

  def getLegalBasesForSelector(
      appId: String,
      selectors: List[String]
  ): F[Map[String, List[LegalBase]]]

  def getRetentionPoliciesForSelector(
      appId: String,
      selectors: List[String]
  ): F[Map[String, List[RetentionPolicy]]]

  def getDataCategories(appId: String): F[List[DataCategory]]

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): F[List[ProcessingCategory]]

  def getProvenances(appId: String, userIds: List[DataSubject]): F[List[Provenance]]

  def getPurposes(appId: String, userIds: List[DataSubject]): F[List[Purpose]]
}
