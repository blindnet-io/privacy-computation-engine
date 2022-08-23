package io.blindnet.pce
package db.repositories.events

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
import java.time.Instant
import priv.TimelineEvent.*

trait EventsRepository {
  def getTimeline(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Timeline]

  def addConsentGiven(cId: UUID, ds: DataSubject, date: Instant): IO[Unit]

  def addLegalBaseEvent(lbId: UUID, ds: DataSubject, e: EventTerms, date: Instant): IO[Unit]
}

object EventsRepository {

  def live(xa: Transactor[IO]): EventsRepository =
    new EventsRepositoryLive(xa)

}
