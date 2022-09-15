package io.blindnet.pce
package db.repositories.events

import java.time.Instant
import java.util.UUID
import javax.xml.crypto.Data

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

class EventsRepositoryLive(xa: Transactor[IO]) extends EventsRepository {

  def getTimeline(ds: DataSubject, ctx: PSContext): IO[Timeline] = {
    val res =
      for {
        lbEvents <- queries.getLegalBaseEvents(ds)
        cgEvents <- queries.getConsentGivenEvents(ds)
        crEvents <- queries.getConsentRevokedEvents(ds)
        oEvents  <- queries.getObjectEvents(ds)
        rEvents  <- queries.getRestrictEvents(ds)
        allEvents = (lbEvents ++ cgEvents ++ crEvents ++ oEvents ++ rEvents).sortBy(_.getTimestamp)
      } yield Timeline.create(allEvents)(ctx)

    res.transact(xa)
  }

  def addConsentGiven(cId: UUID, ds: DataSubject, date: Instant): IO[Unit] =
    queries.addConsentGiven(cId, ds, date).transact(xa).void

  def addConsentRevoked(cId: UUID, ds: DataSubject, date: Instant): IO[Unit] =
    queries.addConsentRevoked(cId, ds, date).transact(xa).void

  def addObject(dId: UUID, ds: DataSubject, date: Instant): IO[Unit] =
    queries.addObject(dId, ds, date).transact(xa).void

  def addRestrict(dId: UUID, ds: DataSubject, date: Instant): IO[Unit] =
    queries.addRestrict(dId, ds, date).transact(xa).void

  def addLegalBaseEvent(lbId: UUID, ds: DataSubject, e: EventTerms, date: Instant): IO[Unit] =
    queries.addLegalBaseEvent(lbId, ds, e, date).transact(xa).void

}
