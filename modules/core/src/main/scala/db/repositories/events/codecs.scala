package io.blindnet.pce
package db.repositories.events

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

  given Read[TimelineEvent.LegalBase] =
    Read[(UUID, LegalBaseTerms, Instant, EventTerms, List[String], List[String], List[String])]
      .map {
        case (lbid, lbType, date, event, dcs, pcs, pps) =>
          TimelineEvent.LegalBase(lbid, event, lbType, date, PrivacyScope.unsafe(dcs, pcs, pps))
      }

  given Read[TimelineEvent.ConsentGiven] =
    Read[(UUID, UUID, Instant, List[String], List[String], List[String])]
      .map {
        case (id, lbid, date, dcs, pcs, pps) =>
          TimelineEvent.ConsentGiven(lbid, date, PrivacyScope.unsafe(dcs, pcs, pps))
      }

  given Read[TimelineEvent.ConsentRevoked] =
    Read[(UUID, UUID, Instant)]
      .map {
        case (id, lbid, date) =>
          TimelineEvent.ConsentRevoked(lbid, date)
      }

  given Read[TimelineEvent.Object] =
    Read[(UUID, Instant, List[String], List[String], List[String])]
      .map {
        case (id, date, dcs, pcs, pps) =>
          TimelineEvent.Object(date, PrivacyScope.unsafe(dcs, pcs, pps))
      }

  given Read[TimelineEvent.Restrict] =
    Read[(UUID, Instant, List[String], List[String], List[String])]
      .map {
        case (id, date, dcs, pcs, pps) =>
          TimelineEvent.Restrict(date, PrivacyScope.unsafe(dcs, pcs, pps))
      }

}
