package io.blindnet.pce
package model

import java.util.UUID
import org.http4s.Uri
import doobie.util.Read
import doobie.postgres.implicits.*
import io.circe.Json
import java.time.Instant
import cats.effect.std.UUIDGen
import cats.effect.kernel.*
import cats.implicits.*
import cats.effect.IO

case class CommandCreateRecommendation(
    id: UUID,
    dId: UUID,
    timestamp: Instant,
    data: Json
)

object CommandCreateRecommendation {
  def create(dId: UUID, data: Json = Json.Null) =
    for {
      id        <- UUIDGen[IO].randomUUID
      timestamp <- Clock[IO].realTimeInstant
      c = CommandCreateRecommendation(id, dId, timestamp, data)
    } yield c

}