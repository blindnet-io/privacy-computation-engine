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

  given Read[PrivacyResponse] =
    // format: off 
    Read[(UUID, UUID, UUID, Option[UUID], Instant, Action, Status, Option[Motive], Option[String],
      Option[String], Option[String], Option[String], Option[String])]
      .map {
        case (id, prid, did, parent, t, a, s, mot, answer, msg, lang, system, data) =>
          val answ = answer.flatMap(a => parse(a).toOption)
          val incl = List.empty
          PrivacyResponse(id, prid, did, t, a, s, mot, answ, msg, lang, system, parent, incl, data)
      }
    // format: on

  given Read[PrivacyRequest] =
    Read[(UUID, UUID, Option[String], List[String], Instant, Target, Option[String])]
      .map {
        case (id, appId, dsid, pDsIds, t, trg, email) =>
          val ds = dsid.map(DataSubject(_))
          PrivacyRequest(id, appId, t, trg, email, ds, pDsIds, List.empty)
      }

  given Get[Set[DataCategory]] = Get[List[String]].map(_.map(DataCategory(_)).toSet)

  given Read[Demand] =
    Read[(UUID, UUID, Action, Option[String], Option[String])]
      .map { case (id, rid, a, m, l) => Demand(id, rid, a, m, l, List.empty, List.empty) }

}
