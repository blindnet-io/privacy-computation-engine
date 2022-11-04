package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.effect.IO
import priv.privacyrequest.*
import dev.profunktor.redis4cats.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class CBData(aid: UUID, rid: ResponseEventId)
object CBData:
  given Codec[CBData] = deriveCodec

trait CallbacksRepository {
  def set(id: UUID, d: CBData): IO[Unit]

  def get(id: UUID): IO[Option[CBData]]

  def remove(id: UUID): IO[Unit]
}

object CallbacksRepository {
  def live(redis: RedisCommands[IO, String, String]): CallbacksRepository =
    new CallbacksRepository {

      def set(id: UUID, d: CBData): IO[Unit] =
        redis.set(s"callbacks:$id", d.asJson.noSpaces).void

      def get(id: UUID): IO[Option[CBData]] =
        redis.get(s"callbacks:$id").map(_.flatMap(decode[CBData](_).toOption))

      def remove(id: UUID): IO[Unit] =
        redis.del(s"callbacks:$id", id.toString).void

    }

}
