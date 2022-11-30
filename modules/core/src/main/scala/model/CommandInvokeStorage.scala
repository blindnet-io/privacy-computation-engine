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
import doobie.util.meta.Meta

// TODO: duplicate with DataRequestAction
enum StorageAction {
  case Get
  case Delete
}

object StorageAction {
  given Meta[StorageAction] = Meta[String].timap {
    case "get"    => StorageAction.Get
    case "delete" => StorageAction.Delete
  } {
    case StorageAction.Get    => "get"
    case StorageAction.Delete => "delete"
  }

}

case class CommandInvokeStorage(
    id: UUID,
    dId: UUID,
    preId: UUID,
    action: StorageAction,
    timestamp: Instant,
    retries: Int
) {
  def addRetry = this.copy(retries = this.retries + 1)
}

object CommandInvokeStorage {
  def create(dId: UUID, preId: UUID, action: StorageAction) =
    for {
      id        <- UUIDGen[IO].randomUUID
      timestamp <- Clock[IO].realTimeInstant
      c = CommandInvokeStorage(id, dId, preId, action, timestamp, 0)
    } yield c

  def createGet(dId: UUID, preId: UUID)    = create(dId, preId, StorageAction.Get)
  def createDelete(dId: UUID, preId: UUID) = create(dId, preId, StorageAction.Delete)
}
