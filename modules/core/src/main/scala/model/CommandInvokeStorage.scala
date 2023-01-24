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

enum StorageAction {
  case Get
  case Delete
  case PrivacyScopeChange
}

object StorageAction {
  given Meta[StorageAction] = Meta[String].timap {
    case "get"           => StorageAction.Get
    case "delete"        => StorageAction.Delete
    case "privacy-scope" => StorageAction.PrivacyScopeChange
  } {
    case StorageAction.Get                => "get"
    case StorageAction.Delete             => "delete"
    case StorageAction.PrivacyScopeChange => "privacy-scope"
  }

}

case class CommandInvokeStorage(
    id: UUID,
    dId: UUID,
    preId: UUID,
    action: StorageAction,
    data: Json,
    timestamp: Instant,
    retries: Int
) {
  def addRetry = this.copy(retries = this.retries + 1)
}

object CommandInvokeStorage {
  def create(dId: UUID, preId: UUID, action: StorageAction, data: Json) =
    for {
      id        <- UUIDGen[IO].randomUUID
      timestamp <- Clock[IO].realTimeInstant
      c = CommandInvokeStorage(id, dId, preId, action, data, timestamp, 0)
    } yield c

  def createGet(dId: UUID, preId: UUID, data: Json)    = create(dId, preId, StorageAction.Get, data)
  def createDelete(dId: UUID, preId: UUID, data: Json) =
    create(dId, preId, StorageAction.Delete, data)

  def createPrivacyScope(dId: UUID, preId: UUID, data: Json) =
    create(dId, preId, StorageAction.PrivacyScopeChange, data)

}
