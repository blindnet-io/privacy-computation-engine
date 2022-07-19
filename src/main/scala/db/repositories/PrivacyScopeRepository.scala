package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait PrivacyScopeRepository {
  def getSelectors(appId: String, userIds: List[DataSubject]): IO[List[Selector]]

  def addSelectors(appId: String, selectors: List[Selector]): IO[Unit]

  def deleteSelectors(appId: String, selectorIds: List[String]): IO[Unit]

  def getSelectorPrivacyScope(
      appId: String,
      selectors: List[String]
  ): IO[Map[String, List[PrivacyScope]]]

  def getLegalBasesForSelector(
      appId: String,
      selectors: List[String]
  ): IO[Map[String, List[LegalBase]]]

  def getRetentionPoliciesForSelector(
      appId: String,
      selectors: List[String]
  ): IO[Map[String, List[RetentionPolicy]]]

  def getDataCategories(appId: String): IO[List[DataCategory]]

  def getProcessingCategories(
      appId: String,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]]

  def getProvenances(appId: String, userIds: List[DataSubject]): IO[List[Provenance]]

  def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]]
}

object PrivacyScopeRepository {
  def live(xa: Transactor[IO]): PrivacyScopeRepository =
    new PrivacyScopeRepository {
      def getSelectors(appId: String, userIds: List[DataSubject]): IO[List[Selector]] = ???

      def addSelectors(appId: String, selectors: List[Selector]): IO[Unit] = ???

      def deleteSelectors(appId: String, selectorIds: List[String]): IO[Unit] = ???

      def getSelectorPrivacyScope(
          appId: String,
          selectors: List[String]
      ): IO[Map[String, List[PrivacyScope]]] = ???

      def getLegalBasesForSelector(
          appId: String,
          selectors: List[String]
      ): IO[Map[String, List[LegalBase]]] = ???

      def getRetentionPoliciesForSelector(
          appId: String,
          selectors: List[String]
      ): IO[Map[String, List[RetentionPolicy]]] = ???

      def getDataCategories(appId: String): IO[List[DataCategory]] =
        sql"select term from data_categories where appid = $appId"
          .query[DataCategory]
          .to[List]
          .transact(xa)

      def getProcessingCategories(
          appId: String,
          userIds: List[DataSubject]
      ): IO[List[ProcessingCategory]] = ???

      def getProvenances(appId: String, userIds: List[DataSubject]): IO[List[Provenance]] = ???

      def getPurposes(appId: String, userIds: List[DataSubject]): IO[List[Purpose]] = ???
    }

}
