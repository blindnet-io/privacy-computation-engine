package io.blindnet.pce
package db.repositories.events

import java.util.UUID

import cats.data.NonEmptyList
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.db.DbUtil.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*
import codecs.given
import java.time.Instant

object queries {

  def getLegalBaseEvents(appId: UUID, ds: DataSubject) =
    sql"""
      select lb.id as lbid, lb."type" as lbtype, lbe."date" as "date", lbe."event" as "event",
      array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
      from legal_base_events lbe
        join legal_bases lb on lb.id = lbe.lbid
        join legal_bases_scope lbs on lbs.lbid = lb.id
        join scope s on s.id = lbs.scid
        join data_categories dc on dc.id = s.dcid
        join processing_categories pc on pc.id = s.pcid
        join processing_purposes pp on pp.id = s.ppid
      where dc.active = true and lbe.dsid = ${ds.id} and lb.appid = ${appId}
      group by lbe.id, lb.id, lb."type", lbe."date", lbe."event"
      order by lbe."date" asc
    """
      .query[TimelineEvent.LegalBase]
      .to[List]

  def getConsentGivenEvents(appId: UUID, ds: DataSubject) =
    sql"""
      select cge.id as id, lb.id as lbid, cge."date" as "date",
      array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
      from consent_given_events cge
        join legal_bases lb on lb.id = cge.lbid
        join legal_bases_scope lbs on lbs.lbid = lb.id
        join scope s on s.id = lbs.scid
        join data_categories dc on dc.id = s.dcid
        join processing_categories pc on pc.id = s.pcid
        join processing_purposes pp on pp.id = s.ppid
      where dc.active = true and cge.dsid = ${ds.id} and lb.appid = ${appId}
      group by cge.id, lb.id, cge."date"
      order by cge."date" asc
    """
      .query[TimelineEvent.ConsentGiven]
      .to[List]

  def getConsentRevokedEvents(appId: UUID, ds: DataSubject) =
    sql"""
      select cre.id as id, lb.id as lbid, cre."date" as "date"
      from consent_revoked_events cre
        join legal_bases lb on lb.id = cre.lbid
      where cre.dsid = ${ds.id} and lb.appid = ${appId}
      order by cre."date" asc;
    """
      .query[TimelineEvent.ConsentRevoked]
      .to[List]

  def addConsentGiven(cId: UUID, ds: DataSubject, date: Instant) =
    sql"""
      insert into consent_given_events (id, lbid, dsid, date)
      values (gen_random_uuid(), $cId, ${ds.id}, $date)
    """.update.run

  def addLegalBaseEvent(lbId: UUID, ds: DataSubject, e: EventTerms, date: Instant) =
    sql"""
      insert into legal_base_events (id, lbid, dsid, event, date)
      values (gen_random_uuid(), $lbId, ${ds.id}, ${e.encode}::event_terms, $date)
    """.update.run

}
