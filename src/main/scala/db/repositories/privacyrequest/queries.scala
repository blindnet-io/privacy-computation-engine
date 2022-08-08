package io.blindnet.privacy
package db.repositories.privacyrequest

import cats.data.NonEmptyList
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.privacy.model.vocabulary.request.*
import model.vocabulary.*
import model.vocabulary.terms.*
import io.blindnet.privacy.db.DbUtil.*
import codecs.given

private object queries {

  def requestExist(reqId: String, appId: String, userId: String) =
    sql"""
      select exists (
        select 1 from privacy_requests pr
        where id = $reqId::uuid and appid = $appId::uuid and dsid = $userId
      )
    """
      .query[Boolean]
      .unique

  def getDemand(id: String) =
    sql"""
      select d.id, pr.id, action, message, lang
      from demands d
        join privacy_requests pr on pr.id = d.prid
      where d.id = $id::uuid
    """
      .query[Demand]
      .option

  def getDemands(ids: NonEmptyList[String]) =
    (sql"""
      select d.id, pr.id, action, message, lang
        from demands d
          join privacy_requests pr on pr.id = d.prid
        where
      """
      ++ FragmentsC.inUuid(fr"d.id", ids))
      .query[Demand]
      .to[List]

  def getRequestDemands(reqId: String) =
    sql"""
      select d.id, pr.id, action, message, lang
      from demands d
        join privacy_requests pr on pr.id = d.prid
      where prid = $reqId::uuid
    """
      .query[Demand]
      .to[List]

  def getPrivacyRequest(reqId: String) =
    sql"""
      select id, appid, dsid, date, target, email
      from privacy_requests
      where id = $reqId::uuid
    """
      .query[PrivacyRequest]
      .option

  def getPrivacyRequests(ids: NonEmptyList[String]) =
    (sql"""
      select id, appid, dsid, date, target, email
      from privacy_requests
      where
    """
      ++ FragmentsC.inUuid(fr"id", ids))
      .query[PrivacyRequest]
      .to[List]

  def getAllDemandResponses(reqId: String) =
    sql"""
      with query as (
        select pre.id as id, pr.id as prid, d.id as did, pre.date as date, d.action as action, pre.status as status,
          pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pre.data as data,
          ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
        from privacy_response_events pre
          join privacy_responses pr on pr.id = pre.prid
          join demands d on d.id = pr.did
        where d.prid = $reqId::uuid
      )
      select * from query where r = 1;
    """
      .query[PrivacyResponse]
      .to[List]

  def getDemandResponse(dId: String) =
    // TODO: duplicate code
    sql"""
      with query as (
        select pre.id as id, pr.id as prid, d.id as did, pre.date as date, d.action as action, pre.status as status,
          pre.answer as answer, pre.message as message, pre.lang as lang, pr.system as system, pre.data as data,
          ROW_NUMBER() OVER (PARTITION BY pr.id ORDER BY date DESC) As r
        from privacy_response_events pre
          join privacy_responses pr on pr.id = pre.prid
          join demands d on d.id = pr.did
        where d.id = $dId::uuid
      )
      select * from query where r = 1;
    """
      .query[PrivacyResponse]
      .option

  def storeNewResponse(r: PrivacyResponse) =
    sql"""
      insert into privacy_response_events (id, prid, date, status, message, lang, data, answer)
      values (
        ${r.id}::uuid, ${r.responseId}::uuid, ${r.timestamp}, ${r.status.encode}::status_terms,
        ${r.message}, ${r.lang}, ${r.data}, ${r.answer.map(_.toString)}
      )
    """.update.run

  def storeRecommendation(r: Recommendation) =
    sql"""
      insert into demand_recommendations (id, did, data_categories, date_from, date_to, provenance)
      values (
        ${r.id}::uuid, ${r.dId}::uuid, ${r.dataCategories.map(_.term).toList},
        ${r.dateFrom}, ${r.dateTo}, ${r.provenance.map(_.encode)}::provenance_terms
      )
    """.update.run

  def getRecommendation(dId: String) =
    sql"""
      select id, did, data_categories, date_from, date_to, provenance
      from demand_recommendations
      where did = ${dId}::uuid
    """
      .query[Recommendation]
      .option

  def getAllUserRequestIds(appId: String, userId: String) =
    sql"""
      select id
      from privacy_requests pr
      where pr.dsid = ${userId} and pr.appid = ${appId}::uuid
    """
      .query[String]
      .to[List]

}
