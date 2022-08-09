package io.blindnet.pce
package db.repositories

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import priv.*
import priv.terms.*
import db.DbUtil

trait PrivacyScopeRepository  {
  def getDataCategories(appId: UUID): IO[List[DataCategory]]

  def getProcessingCategories(
      appId: UUID,
      userIds: List[DataSubject]
  ): IO[List[ProcessingCategory]]

  def getPurposes(appId: UUID, userIds: List[DataSubject]): IO[List[Purpose]]

  def getTimeline(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Timeline]
}

// TODO: select for users
object PrivacyScopeRepository {

  given Read[TimelineEvent.LegalBase] =
    Read[(String, LegalBaseTerms, Instant, EventTerms, List[String], List[String], List[String])]
      .map {
        case (lbid, lbType, date, event, dcs, pcs, pps) =>
          val scope = dcs.lazyZip(pcs).lazyZip(pps).map {
            case (dc, pc, pp) => PrivacyScopeTriple.unsafe(dc, pc, pp)
          }
          TimelineEvent.LegalBase(lbid, event, lbType, date, PrivacyScope(scope.toSet))
      }

  given Read[TimelineEvent.ConsentGiven] =
    Read[(String, String, Instant, List[String], List[String], List[String])]
      .map {
        case (id, lbid, date, dcs, pcs, pps) =>
          val scope = dcs.lazyZip(pcs).lazyZip(pps).map {
            case (dc, pc, pp) => PrivacyScopeTriple.unsafe(dc, pc, pp)
          }
          TimelineEvent.ConsentGiven(lbid, date, PrivacyScope(scope.toSet))
      }

  given Read[TimelineEvent.ConsentRevoked] =
    Read[(String, String, Instant)]
      .map {
        case (id, lbid, date) =>
          TimelineEvent.ConsentRevoked(lbid, date)
      }

  object queries {
    def getDataCategories(appId: UUID) =
      sql"""
        select distinct(dc.term) from legal_bases lb
        join legal_bases_scope lbsc on lbsc.lbid = lb.id
        join "scope" s on s.id = lbsc.scid
        join data_categories dc on dc.id = s.dcid
        where lb.active and dc.active and lb.appid = $appId
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

  }

  def live(xa: Transactor[IO]): PrivacyScopeRepository =
    new PrivacyScopeRepository {
      def getDataCategories(appId: UUID): IO[List[DataCategory]] =
        queries
          .getDataCategories(appId)
          .transact(xa)

      def getProcessingCategories(
          appId: UUID,
          userIds: List[DataSubject]
      ): IO[List[ProcessingCategory]] =
        queries
          .getProcessingCategories(appId)
          .transact(xa)

      def getPurposes(appId: UUID, userIds: List[DataSubject]): IO[List[Purpose]] =
        queries.getPurposes(appId).transact(xa)

      // TODO: add restrict and object events
      def getTimeline(appId: UUID, userIds: NonEmptyList[DataSubject]): IO[Timeline] =
        val res =
          for {
            lbEvents <- queries.getLegalBaseEvents(appId, userIds)
            cgEvents <- queries.getConsentGivenEvents(appId, userIds)
            crEvents <- queries.getConsentRevokedEvents(appId, userIds)
            allEvents = (lbEvents ++ cgEvents ++ crEvents).sortBy(_.getTimestamp)
          } yield Timeline(allEvents)

        res.transact(xa)

    }

}
