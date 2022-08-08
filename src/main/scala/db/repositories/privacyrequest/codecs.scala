package io.blindnet.privacy
package db.repositories.privacyrequest

import java.time.Instant

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.privacy.util.extension.*
import io.circe.*
import io.circe.parser.*
import db.DbUtil.*
import model.vocabulary.request.*
import model.vocabulary.*
import model.vocabulary.terms.*

private object codecs {

  given Read[PrivacyResponse] =
    Read[
      (
          String,
          String,
          String,
          Instant,
          Action,
          Status,
          Option[String],
          Option[String],
          Option[String],
          Option[String],
          Option[String]
      )
    ]
      .map {
        case (id, prid, did, t, a, s, answer, msg, lang, system, data) =>
          val answ = answer.flatMap(a => parse(a).toOption)
          val incl = List.empty
          PrivacyResponse(id, prid, did, t, a, s, answ, msg, lang, system, incl, data)
      }

  given Read[PrivacyRequest] =
    Read[(String, String, String, Instant, Target, Option[String])]
      .map {
        case (id, appId, dsid, t, trg, email) =>
          val ds = List(DataSubject(dsid, ""))
          PrivacyRequest(id, appId, t, trg, email, ds, List.empty)
      }

  given Get[Set[DataCategory]] = Get[List[String]].map(_.map(DataCategory(_)).toSet)

  given Read[Demand] =
    Read[(String, String, Action, Option[String], Option[String])]
      .map { case (id, rid, a, m, l) => Demand(id, rid, a, m, l, List.empty, List.empty) }

}
