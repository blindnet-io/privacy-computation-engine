package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID

import cats.effect.kernel.{ Clock, Sync }
import cats.effect.std.UUIDGen
import cats.implicits.*
import doobie.*
import doobie.postgres.implicits.*
import doobie.util.Get
import io.circe.*
import io.circe.generic.semiauto.*
import terms.*

opaque type ResponseId = UUID
object ResponseId:
  def apply(uuid: UUID): ResponseId          = uuid
  extension (id: ResponseId) def value: UUID = id

opaque type ResponseEventId = UUID
object ResponseEventId:
  def apply(uuid: UUID): ResponseEventId          = uuid
  extension (id: ResponseEventId) def value: UUID = id
  given Decoder[ResponseEventId]                  = Decoder.decodeUUID.map(ResponseEventId.apply)
  given Encoder[ResponseEventId]                  = Encoder.encodeUUID.contramap(_.value)

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
        id   <- UUIDGen[F].randomUUID.map(ResponseId.apply)
        evId <- UUIDGen[F].randomUUID.map(ResponseEventId.apply)
        // format: off
        resp = PrivacyResponse(id, evId, dId, pr.timestamp, action, Status.UnderReview, parent = parent)
        // format: on
      } yield resp

    pr.demands.traverse(
      d =>
        val children = d.action.granularize().filterNot(_ == d.action)
        if children.length > 0 then
          for {
            resp     <- createResponse(d.id, d.action)
            includes <- children.traverse(a => createResponse(d.id, a, Some(resp.id)))
          } yield resp.copy(includes = includes)
        else createResponse(d.id, d.action)
    )

  }

  // TODO: optimize with HashMap
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
