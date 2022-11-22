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

  def getDataCategories(appId: UUID, withSelectors: Boolean = true): IO[Set[DataCategory]] =
    if withSelectors then {
      val q = for {
        selectors <- queries.getSelectors(appId, true)
        dcs       <- queries.getDataCategories(appId, withSelectors)
        res = dcs.flatMap(DataCategory.granularize(_, selectors))
      } yield res
      q.transact(xa)
    } else queries.getDataCategories(appId, withSelectors).transact(xa)

  def getAllDataCategories(appId: UUID): IO[Set[DataCategory]] =
    queries.getAllDataCategories(appId).transact(xa)

  def getProcessingCategories(appId: UUID): IO[Set[ProcessingCategory]] =
    queries
      .getProcessingCategories(appId)
      .map(_.flatMap(ProcessingCategory.granularize))
      .transact(xa)

  def getPurposes(appId: UUID): IO[Set[Purpose]] =
    queries.getPurposes(appId).map(_.flatMap(Purpose.granularize)).transact(xa)

  def getContext(appId: UUID): IO[PSContext] = {
    val ctx = for {
      selectors <- queries.getSelectors(appId, true)
    } yield PSContext(selectors)

    ctx.transact(xa)
  }

  def getSelectors(appId: UUID, active: Boolean): IO[Set[DataCategory]] =
    queries.getSelectors(appId, active).transact(xa)

  def addSelectors(appId: UUID, terms: NonEmptyList[(UUID, DataCategory)]): IO[Unit] =
    (queries.addSelectors(appId, terms) *> queries.addScopeForSelectors(terms.map(_._1)))
      .transact(xa)
      .void

}
