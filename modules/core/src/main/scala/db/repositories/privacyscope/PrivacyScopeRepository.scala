package io.blindnet.pce
package db.repositories.privacyscope

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import priv.*
import priv.terms.*
import db.DbUtil

trait PrivacyScopeRepository {
  def getDataCategories(appId: UUID, selectors: Boolean = true): IO[List[DataCategory]]

  def getProcessingCategories(appId: UUID): IO[List[ProcessingCategory]]

  def getPurposes(appId: UUID): IO[List[Purpose]]

  def getTimeline(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Timeline]

  def getSelectors(appId: UUID, active: Boolean): IO[List[DataCategory]]

  def addSelectors(appId: UUID, terms: NonEmptyList[(UUID, DataCategory)]): IO[Unit]

  def addRetentionPolicies(appId: UUID, rs: NonEmptyList[(DataCategory, RetentionPolicy)]): IO[Unit]

}

object PrivacyScopeRepository {

  def live(xa: Transactor[IO]): PrivacyScopeRepository =
    new PrivacyScopeRepositoryLive(xa)

}
