package io.blindnet.pce
package db.repositories.privacyrequest

import java.time.Instant
import java.util.UUID

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.util.extension.*
import io.circe.*
import io.circe.parser.*
import db.DbUtil.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*

private object codecs {

  given Meta[ResponseId]      = Meta[UUID].timap(ResponseId.apply)(x => x.value)
  given Meta[ResponseEventId] = Meta[UUID].timap(ResponseEventId.apply)(x => x.value)
  given Meta[RequestId]       = Meta[UUID].timap(RequestId.apply)(x => x.value)

  given Read[PrivacyResponse] =
    // format: off 
    Read[(ResponseId, ResponseEventId, UUID, Option[ResponseId], Instant, Action, Status, Option[Motive], Option[String],
      Option[String], Option[String], Option[String], Option[String])]
      .map {
        case (id, evid, did, parent, t, a, s, mot, answer, msg, lang, system, data) =>
          val answ = answer.flatMap(a => parse(a).toOption)
          val incl = List.empty
          PrivacyResponse(id, evid, did, t, a, s, mot, answ, msg, lang, system, parent, incl, data)
      }
    // format: on

  given Read[PrivacyRequest] =
    Read[(RequestId, UUID, Option[String], List[String], Instant, Target, Option[String])]
      .map {
        case (id, appId, dsid, pDsIds, t, trg, email) =>
          val ds = dsid.map(DataSubject(_, appId))
          PrivacyRequest(id, appId, t, trg, email, ds, pDsIds, List.empty)
      }

  given Get[Set[DataCategory]] = Get[List[String]].map(_.map(DataCategory(_)).toSet)

  given Read[Demand] =
    Read[(UUID, RequestId, Action, Option[String], Option[String])]
      .map { case (id, rid, a, m, l) => Demand(id, rid, a, m, l, List.empty, List.empty) }

}
