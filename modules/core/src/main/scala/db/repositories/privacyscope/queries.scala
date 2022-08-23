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

  def getAllDataCategories(appId: UUID) =
    sql"""
      select term from data_categories
      where (active = true or active is null) and (appid = $appId or appid is null)
    """
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

}
