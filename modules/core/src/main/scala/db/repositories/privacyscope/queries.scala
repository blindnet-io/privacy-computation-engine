package io.blindnet.pce
package db.repositories.privacyscope

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

object queries {
  def getDataCategories(appId: UUID, selectors: Boolean) =
    (fr"""
      select distinct(dc.term) from legal_bases lb
      join legal_bases_scope lbsc on lbsc.lbid = lb.id
      join "scope" s on s.id = lbsc.scid
      join data_categories dc on dc.id = s.dcid
      where lb.active and dc.active and lb.appid = $appId
    """ ++ (if selectors then fr"" else fr" and selector = false"))
      .query[DataCategory]
      .to[List]

  def getProcessingCategories(appId: UUID) =
    sql"""
      select distinct(pc.term) from legal_bases lb
      join legal_bases_scope lbsc on lbsc.lbid = lb.id
      join "scope" s on s.id = lbsc.scid
      join processing_categories pc on pc.id = s.pcid
      where lb.active and lb.appid = $appId
    """
      .query[ProcessingCategory]
      .to[List]

  def getPurposes(appId: UUID) =
    sql"""
      select distinct(pp.term) from legal_bases lb
      join legal_bases_scope lbsc on lbsc.lbid = lb.id
      join "scope" s on s.id = lbsc.scid
      join processing_purposes pp on pp.id = s.ppid
      where lb.active and lb.appid = $appId
    """
      .query[Purpose]
      .to[List]

  def getLegalBaseEvents(appId: UUID, userIds: NonEmptyList[DataSubject]) =
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
      where dc.active = true and lbe.dsid = ${userIds.head.id} and lb.appid = ${appId}
      group by lbe.id, lb.id, lb."type", lbe."date", lbe."event"
      order by lbe."date" asc
    """
      .query[TimelineEvent.LegalBase]
      .to[List]

  def getConsentGivenEvents(appId: UUID, userIds: NonEmptyList[DataSubject]) =
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
      where dc.active = true and cge.dsid = ${userIds.head.id} and lb.appid = ${appId}
      group by cge.id, lb.id, cge."date"
      order by cge."date" asc
    """
      .query[TimelineEvent.ConsentGiven]
      .to[List]

  def getConsentRevokedEvents(appId: UUID, userIds: NonEmptyList[DataSubject]) =
    sql"""
      select cre.id as id, lb.id as lbid, cre."date" as "date"
      from consent_revoked_events cre
        join legal_bases lb on lb.id = cre.lbid
      where cre.dsid = ${userIds.head.id} and lb.appid = ${appId}
      order by cre."date" asc;
    """
      .query[TimelineEvent.ConsentRevoked]
      .to[List]

  def getSelectors(appId: UUID, active: Boolean) =
    sql"""
      select term
      from data_categories
      where selector = true and appid = $appId and active = $active
    """
      .query[DataCategory]
      .to[List]

  def addSelectors(appId: UUID, terms: NonEmptyList[(UUID, DataCategory)]) = {
    val sql = s"""
      insert into data_categories (id, term, selector, appid, active)
      values (?, ?, true, '$appId', true)
    """
    Update[(UUID, DataCategory)](sql).updateMany(terms)
  }

  def addScopeForSelectors(sId: NonEmptyList[UUID]) = {
    val sql = fr"""
      insert into scope (id, dcid, pcid, ppid)
      (
        select gen_random_uuid(), dc.id, pc.id, pp.id
        from data_categories dc, processing_categories pc, processing_purposes pp
        where
    """ ++ Fragments.in(fr"dc.id", sId) ++ fr")"
    sql.update.run
  }

  def addRetentionPolicies(appId: UUID, rs: NonEmptyList[(DataCategory, RetentionPolicy)]) = {
    val sql = s"""
      insert into retention_policies (id, appid, dcid, policy, duration, after)
      values ( gen_random_uuid(), '$appId', (select id from data_categories where term = ?), ?::policy_terms, ?, ?::event_terms)
    """
    Update[(DataCategory, String, String, String)](sql)
      .updateMany(rs.map(r => (r._1, r._2.policyType.encode, r._2.duration, r._2.after.encode)))
  }

}
