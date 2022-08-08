package io.blindnet.privacy
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.std.Queue
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.db.DbUtil
import io.blindnet.privacy.util.extension.*
import model.vocabulary.*
import model.vocabulary.terms.*

trait PendingRequestsRepository {
  def add(reqId: UUID): IO[Unit]

  def get(): IO[Option[UUID]]
}

object PendingRequestsRepository {
  def live(): IO[PendingRequestsRepository] =
    for {
      pendingRequests <- Queue.unbounded[IO, UUID]
    } yield new PendingRequestsRepository {

      def add(reqId: UUID): IO[Unit] =
        pendingRequests.offer(reqId)

      def get(): IO[Option[UUID]] =
        pendingRequests.tryTake

    }

}
