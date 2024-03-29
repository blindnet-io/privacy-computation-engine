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
import priv.TimelineEvent.*

trait EventsRepository {
  def getTimeline(ds: DataSubject, ctx: PSContext): IO[Timeline]

  def getTimelineNoScope(ds: DataSubject): IO[Timeline]

  def addConsentGiven(cId: UUID, ds: DataSubject, date: Instant): IO[Unit]

  def addConsentRevoked(cId: UUID, ds: DataSubject, date: Instant): IO[Unit]

  def addObject(dId: UUID, ds: DataSubject, date: Instant): IO[Unit]

  def addRestrict(dId: UUID, ds: DataSubject, date: Instant): IO[Unit]

  def addLegalBaseEvent(lbId: UUID, ds: DataSubject, e: EventTerms, date: Instant): IO[Unit]
}

object EventsRepository {

  def live(xa: Transactor[IO]): EventsRepository =
    new EventsRepositoryLive(xa)

}
