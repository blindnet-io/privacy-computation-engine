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

  def getObjectEvents(appId: UUID, ds: DataSubject) =
    sql"""
      select oe.id as id, oe."date" as "date", array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
      from object_events oe
        join demands d on d.id = oe.did
        join privacy_requests pr on pr.id = d.prid
        join demand_restrictions dr on dr.did = d.id
        join demand_restriction_scope drs on drs.drid = dr.id
        join scope s on s.id = drs.scid
        join data_categories dc on dc.id = s.dcid
        join processing_categories pc on pc.id = s.pcid
        join processing_purposes pp on pp.id = s.ppid
      where dr."type" = 'PRIVACY_SCOPE' and oe.dsid = ${ds.id} and pr.appid = ${appId}
      group by oe.id, oe."date"
      order by oe."date" asc
    """
      .query[TimelineEvent.Object]
      .to[List]

  def getRestrictEvents(appId: UUID, ds: DataSubject) =
    sql"""
      select re.id as id, re."date" as "date", array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
      from restrict_events re
        join demands d on d.id = re.did
        join privacy_requests pr on pr.id = d.prid
        join demand_restrictions dr on dr.did = d.id
        join demand_restriction_scope drs on drs.drid = dr.id
        join scope s on s.id = drs.scid
        join data_categories dc on dc.id = s.dcid
        join processing_categories pc on pc.id = s.pcid
        join processing_purposes pp on pp.id = s.ppid
      where dr."type" = 'PRIVACY_SCOPE' and re.dsid = ${ds.id} and pr.appid = ${appId}
      group by re.id, re."date"
      order by re."date" asc
    """
      .query[TimelineEvent.Restrict]
      .to[List]

  def addConsentGiven(cId: UUID, ds: DataSubject, date: Instant) =
    sql"""
      insert into consent_given_events (id, lbid, dsid, date)
      values (gen_random_uuid(), $cId, ${ds.id}, $date)
    """.update.run

  def addConsentRevoked(cId: UUID, ds: DataSubject, date: Instant) =
    sql"""
      insert into consent_revoked_events (id, lbid, dsid, date)
      values (gen_random_uuid(), $cId, ${ds.id}, $date)
    """.update.run

  def addObject(dId: UUID, ds: DataSubject, date: Instant) =
    sql"""
      insert into object_events (id, did, dsid, date)
      values (gen_random_uuid(), $dId, ${ds.id}, $date)
    """.update.run

  def addRestrict(dId: UUID, ds: DataSubject, date: Instant) =
    sql"""
      insert into restrict_events (id, did, dsid, date)
      values (gen_random_uuid(), $dId, ${ds.id}, $date)
    """.update.run

  def addLegalBaseEvent(lbId: UUID, ds: DataSubject, e: EventTerms, date: Instant) =
    sql"""
      insert into legal_base_events (id, lbid, dsid, event, date)
      values (gen_random_uuid(), $lbId, ${ds.id}, ${e.encode}::event_terms, $date)
    """.update.run

}
