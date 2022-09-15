package io.blindnet.pce
package model

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import cats.effect.kernel.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import doobie.postgres.implicits.*
import doobie.util.Read
import io.circe.Json
import org.http4s.Uri

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
