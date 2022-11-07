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
import io.blindnet.identityclient.auth.StRepository
import scala.concurrent.duration.*
import io.blindnet.identityclient.auth.St

case class DashboardToken(appId: UUID, token: String) extends St

trait DashboardTokensRepository extends StRepository[DashboardToken, IO] {
  def set(appId: UUID, token: String): IO[Unit]

  def findByToken(token: String): IO[Option[DashboardToken]]
}

object DashboardTokensRepository {
  def live(redis: RedisCommands[IO, String, String]): DashboardTokensRepository =
    new DashboardTokensRepository {

      def set(appId: UUID, token: String): IO[Unit] =
        redis.setEx(s"dashboard_token:$token", appId.toString, 1.hour).void

      def findByToken(token: String): IO[Option[DashboardToken]] =
        redis
          .get(s"dashboard_token:$token")
          .map(_.map(UUID.fromString.andThen(DashboardToken(_, token))))

    }

}
