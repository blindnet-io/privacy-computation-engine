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

case class PrivacyResponse(
    id: UUID,
    responseId: UUID,
    demandId: UUID,
    timestamp: Instant,
    action: Action,
    status: Status,
    motive: Option[Motive] = None,
    answer: Option[Json] = None,
    message: Option[String] = None,
    lang: Option[String] = None,
    system: Option[String] = None,
    parent: Option[UUID] = None,
    includes: List[PrivacyResponse] = List.empty,
    data: Option[String] = None
)

object PrivacyResponse {
  def fromPrivacyRequest[F[_]: Sync](pr: PrivacyRequest) = {

    def createResponse(dId: UUID, action: Action, parent: Option[UUID] = None) =
      for {
        id  <- UUIDGen[F].randomUUID
        rId <- UUIDGen[F].randomUUID
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
            includes <- children.traverse(a => createResponse(d.id, a, Some(resp.responseId)))
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
        case (Nil, Nil)    => parents
        case (Nil, h :: t) =>
          val p = parents.map(
            a =>
              if h.parent.contains(a.responseId) then a.copy(includes = h :: a.includes)
              else a
          )
          loop(all, cur, t, p)
        case (h :: t, _) if all.exists(_.parent.contains(h.responseId)) || h.parent.isEmpty =>
          loop(all, t, children, h :: parents)
        case (h :: t, _)                                                                    =>
          loop(all, t, h :: children, parents)
      }

    val parents = loop(rs, rs, List.empty, List.empty)
    if rs.map(_.responseId) == parents.map(_.responseId) then parents
    else loop(parents, parents, List.empty, List.empty)
  }

}
