package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.pce.db.DbUtil
import io.blindnet.pce.util.extension.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*

trait CallbacksRepository {
  def set(id: UUID, appId: UUID, respEvId: ResponseEventId): IO[Unit]

  def get(id: UUID): IO[Option[(UUID, ResponseEventId)]]

  def remove(id: UUID): IO[Unit]
}

object CallbacksRepository {
  def live(): IO[CallbacksRepository] =
    for {
      callbacks <- Ref.of[IO, Map[UUID, (UUID, ResponseEventId)]](Map.empty)
    } yield new CallbacksRepository {

      def set(id: UUID, appId: UUID, respEvId: ResponseEventId): IO[Unit] =
        callbacks.update(_.updated(id, (appId, respEvId)))

      def get(id: UUID): IO[Option[(UUID, ResponseEventId)]] =
        callbacks.get.map(_.get(id))

      def remove(id: UUID): IO[Unit] =
        callbacks.update(_.removed(id))

    }

}
