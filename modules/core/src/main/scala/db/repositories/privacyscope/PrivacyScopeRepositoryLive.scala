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

// TODO: select for users
class PrivacyScopeRepositoryLive(xa: Transactor[IO]) extends PrivacyScopeRepository {

  def getDataCategories(appId: UUID, selectors: Boolean = true): IO[List[DataCategory]] =
    queries
      .getDataCategories(appId, selectors)
      .transact(xa)

  def getProcessingCategories(appId: UUID): IO[List[ProcessingCategory]] =
    queries
      .getProcessingCategories(appId)
      .transact(xa)

  def getPurposes(appId: UUID): IO[List[Purpose]] =
    queries.getPurposes(appId).transact(xa)

  // TODO: add restrict and object events
  def getTimeline(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Timeline] =
    val res =
      for {
        lbEvents <- queries.getLegalBaseEvents(appId, userIds)
        cgEvents <- queries.getConsentGivenEvents(appId, userIds)
        crEvents <- queries.getConsentRevokedEvents(appId, userIds)
        allEvents = (lbEvents ++ cgEvents ++ crEvents).sortBy(_.getTimestamp)
      } yield Timeline(allEvents)

    res.transact(xa)

  def getSelectors(appId: UUID, active: Boolean): IO[List[DataCategory]] =
    queries.getSelectors(appId, active).transact(xa)

  def addSelectors(appId: UUID, terms: NonEmptyList[(UUID, DataCategory)]): IO[Unit] =
    (queries.addSelectors(appId, terms) *> queries.addScopeForSelectors(terms.map(_._1)))
      .transact(xa)
      .void

}
