package io.blindnet.pce
package db.repositories.privacyrequest

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

private object queries {

  def requestExist(reqId: RequestId, appId: UUID, userId: Option[String]) =
    (fr"""
      select exists (
        select 1 from privacy_requests pr
        where id = $reqId and appid = $appId and
    """ ++ userId.map(id => fr"(dsid is null or dsid = $id)").getOrElse(fr"dsid is null")
      ++ fr")")
      .query[Boolean]
      .unique

  def demandExist(appId: UUID, dId: UUID) =
    sql"""
      select exists (
        select 1 from demands d
        	join privacy_requests pr on pr.id = d.prid
        where d.id = $dId and pr.appid = $appId
      )
    """
      .query[Boolean]
      .unique

  def getDemand(id: UUID) =
    sql"""
      select d.id, pr.id, action, message, lang
      from demands d
        join privacy_requests pr on pr.id = d.prid
      where d.id = $id
    """
      .query[Demand]
      .option

  def getDemands(ids: NonEmptyList[UUID]) =
    (sql"""
      select d.id, pr.id, action, message, lang
        from demands d
          join privacy_requests pr on pr.id = d.prid
        where
      """
      ++ Fragments.in(fr"d.id", ids))
      .query[Demand]
      .to[List]

  def getRequestDemands(reqId: RequestId) =
    sql"""
      select d.id, pr.id, action, message, lang
      from demands d
        join privacy_requests pr on pr.id = d.prid
      where prid = $reqId
    """
      .query[Demand]
      .to[List]

  def getDemandRestrictions(dId: UUID) =
    sql"""
      select dr.type, dr.cid, dr.from_date, dr.to_date, dr.provenance_term, dr.target_term, dr.data_reference, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
      from demand_restrictions dr
        left join demand_restriction_scope drs on drs.drid = dr.id
        left join "scope" s on s.id = drs.scid
        left join data_categories dc on dc.id = s.dcid 
        left join processing_categories pc on pc.id = s.pcid 
        left join processing_purposes pp on pp.id = s.ppid 
      where did = $dId
      group by dr.type, dr.cid, dr.from_date, dr.to_date, dr.provenance_term, dr.target_term, dr.data_reference
    """
      .query[Restriction]
      .to[List]

  def getPrivacyRequest(reqId: RequestId) =
    sql"""
      select id, appid, dsid, provided_dsids, date, target, email
      from privacy_requests
      where id = $reqId
    """
      .query[PrivacyRequest]
      .option

  def getPrivacyRequestFromDemand(dId: UUID) =
    sql"""
      select id, appid, dsid, provided_dsids, date, target, email
      from privacy_requests
      where id = (select d.prid from demands d where d.id = $dId)
    """
      .query[PrivacyRequest]
      .option

  def getPrivacyRequests(ids: NonEmptyList[RequestId]) =
    (sql"""
      select id, appid, dsid, provided_dsids, date, target, email
      from privacy_requests
      where
    """
      ++ Fragments.in(fr"id", ids))
      .query[PrivacyRequest]
      .to[List]

  def getAllDemandResponses(reqId: RequestId) =
    sql"""
      with query as (
        select pr.id as prid, pre.id as id, d.id as did, pr.parent as parent, pre.date as date, pr.action as action, pre.status as status,
          pre.motive as motive, pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pred.data as data,
          ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
        from privacy_response_events pre
          join privacy_responses pr on pr.id = pre.prid
          join demands d on d.id = pr.did
          left join privacy_response_events_data pred on pred.preid = pre.id
        where d.prid = $reqId
      )
      select * from query where r = 1;
    """
      .query[PrivacyResponse]
      .to[List]

  def getDemandResponses(dId: UUID) =
    // TODO: duplicate code
    sql"""
      with query as (
        select pr.id as prid, pre.id as id, d.id as did, pr.parent as parent, pre.date as date, pr.action as action, pre.status as status,
          pre.motive as motive, pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pred.data as data,
          ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
        from privacy_response_events pre
          join privacy_responses pr on pr.id = pre.prid
          join demands d on d.id = pr.did
          left join privacy_response_events_data pred on pred.preid = pre.id
        where d.id = $dId
      )
      select * from query where r = 1;
    """
      .query[PrivacyResponse]
      .to[List]

  def getResponse(respId: UUID) =
    sql"""
      select pr.id as prid, pre.id as id, d.id as did, pr.parent as parent, pre.date as date, pr.action as action, pre.status as status,
        pre.motive as motive, pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pred.data as data
      from privacy_response_events pre
        join privacy_responses pr on pr.id = pre.prid
        join demands d on d.id = pre.did
        left join privacy_response_events_data pred on pred.preid = pre.id
      where pre.id = $respId
    """
      .query[PrivacyResponse]
      .option

  def storeNewResponse(r: PrivacyResponse) =
    sql"""
      insert into privacy_response_events (id, prid, date, status, motive, message, lang, answer)
      values (
        ${r.eventId}, ${r.id}, ${r.timestamp}, ${r.status.encode}::status_terms,
        ${r.motive.map(_.encode)}::motive_terms, ${r.message}, ${r.lang},
        ${r.answer.map(_.toString)}
      )
    """.update.run

  def storeResponseData(preId: ResponseEventId, data: Option[String]) =
    sql"""
      insert into privacy_response_events_data (preid, data)
      values ($preId, $data)
    """.update.run

  def storeRecommendation(r: Recommendation) =
    sql"""
      insert into demand_recommendations (id, did, status, motive, data_categories, date_from, date_to, provenance, target)
      values (
        ${r.id}, ${r.dId}, ${r.status.map(_.encode)}::status_terms,
        ${r.motive.map(_.encode)}::motive_terms, ${r.dataCategories.map(_.term).toList},
        ${r.dateFrom}, ${r.dateTo}, ${r.provenance.map(_.encode)}::provenance_terms,
        ${r.target.map(_.encode)}::target_terms
      )
    """.update.run

  def getRecommendation(dId: UUID) =
    sql"""
      select id, did, status, motive, data_categories, date_from, date_to, provenance, target
      from demand_recommendations
      where did = $dId
    """
      .query[Recommendation]
      .option

  def getAllUserRequestIds(appId: UUID, userId: String) =
    sql"""
      select id
      from privacy_requests pr
      where pr.dsid = $userId and pr.appid = $appId
    """
      .query[RequestId]
      .to[List]

  def getDataSubject(dId: UUID) =
    sql"""
      select ds.id, ds.schema
      from data_subjects ds
        join privacy_requests pr on pr.dsid = ds.id
        join demands d on d.prid = pr.id
      where d.id = $dId
    """
      .query[DataSubject]
      .option

}
