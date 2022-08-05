package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.privacy.db.DbUtil
import model.vocabulary.*
import model.vocabulary.terms.*
import io.blindnet.privacy.util.extension.*
import cats.effect.std.Queue

trait PendingRequestsRepository {
  def add(reqId: String): IO[Unit]

  def get(): IO[Option[String]]
}

object PendingRequestsRepository {
  def live(): IO[PendingRequestsRepository] =
    for {
      pendingRequests <- Queue.unbounded[IO, String]
    } yield new PendingRequestsRepository {

      def add(reqId: String): IO[Unit] =
        pendingRequests.offer(reqId)

      def get(): IO[Option[String]] =
        pendingRequests.tryTake

    }

}
