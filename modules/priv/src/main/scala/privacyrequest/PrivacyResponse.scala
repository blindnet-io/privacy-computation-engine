package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID

import io.circe.Json
import terms.*
import cats.effect.kernel.Sync
import cats.effect.std.UUIDGen
import cats.effect.kernel.Clock
import cats.implicits.*
import doobie.util.Get
import doobie.*
import doobie.postgres.implicits.*

opaque type ResponseId = UUID
object ResponseId:
  def apply(uuid: UUID): ResponseId          = uuid
  extension (id: ResponseId) def value: UUID = id

opaque type ResponseEventId = UUID
object ResponseEventId:
  def apply(uuid: UUID): ResponseEventId          = uuid
  extension (id: ResponseEventId) def value: UUID = id

case class PrivacyResponse(
    id: ResponseId,
    eventId: ResponseEventId,
    demandId: UUID,
    timestamp: Instant,
    action: Action,
    status: Status,
    motive: Option[Motive] = None,
    answer: Option[Json] = None,
    message: Option[String] = None,
    lang: Option[String] = None,
    system: Option[String] = None,
    parent: Option[ResponseId] = None,
    includes: List[PrivacyResponse] = List.empty,
    data: Option[String] = None
)

object PrivacyResponse {
  def fromPrivacyRequest[F[_]: Sync](pr: PrivacyRequest) = {

    def createResponse(dId: UUID, action: Action, parent: Option[ResponseId] = None) =
      for {
        id  <- UUIDGen[F].randomUUID.map(ResponseId.apply)
        rId <- UUIDGen[F].randomUUID.map(ResponseEventId.apply)
        // format: off
        resp = PrivacyResponse(id, rId, dId, pr.timestamp, action, Status.UnderReview, parent = parent)
        // format: on
      } yield resp

    pr.demands.traverse(
      d =>
        val children = d.action.getMostGranularSubcategories()
        if children.length > 0 then
          for {
            resp     <- createResponse(d.id, d.action)
            includes <- children.traverse(a => createResponse(d.id, a, Some(resp.id)))
          } yield resp.copy(includes = includes)
        else createResponse(d.id, d.action)
    )

  }

  // TODO: optimize with Map
  def group(rs: List[PrivacyResponse]): List[PrivacyResponse] = {
    def loop(
        all: List[PrivacyResponse],
        cur: List[PrivacyResponse],
        children: List[PrivacyResponse],
        parents: List[PrivacyResponse]
    ): List[PrivacyResponse] =
      (cur, children) match {
        case (Nil, Nil)                                                             => parents
        case (Nil, h :: t)                                                          =>
          val p = parents.map(
            a =>
              if h.parent.contains(a.id) then a.copy(includes = h :: a.includes)
              else a
          )
          loop(all, cur, t, p)
        case (h :: t, _) if all.exists(_.parent.contains(h.id)) || h.parent.isEmpty =>
          loop(all, t, children, h :: parents)
        case (h :: t, _)                                                            =>
          loop(all, t, h :: children, parents)
      }

    val parents = loop(rs, rs, List.empty, List.empty)
    if rs.map(_.id) == parents.map(_.id) then parents
    else loop(parents, parents, List.empty, List.empty)
  }

}
