package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import priv.*
import priv.terms.*
import cats.data.NonEmptyList

trait LegalBaseRepository {
  def get(appId: UUID, scope: Boolean = true): IO[List[LegalBase]]

  def get(appId: UUID, ds: DataSubject): IO[List[LegalBase]]

  def get(appId: UUID, lbId: UUID, scope: Boolean): IO[Option[LegalBase]]

  def get(appId: UUID, lbId: NonEmptyList[UUID]): IO[List[LegalBase]]

  def getByScope(appId: UUID, scope: PrivacyScope): IO[Option[UUID]]

  def add(appId: UUID, lb: LegalBase, proactive: Boolean = false): IO[Unit]

  def addScope(appId: UUID, lbId: UUID, scope: PrivacyScope): IO[Unit]
}

object LegalBaseRepository {

  given Read[LegalBase] =
    Read[
      (
          UUID,
          LegalBaseTerms,
          Option[String],
          Option[String],
          Boolean,
          List[String],
          List[String],
          List[String]
      )
    ]
      .map {
        case (id, lbTerm, name, desc, active, dcs, pcs, pps) => {
          LegalBase(id, lbTerm, PrivacyScope.unsafe(dcs, pcs, pps), name, desc, active)
        }
      }

  def live(xa: Transactor[IO], pools: Pools): LegalBaseRepository =
    new LegalBaseRepository {
      def get(appId: UUID, scope: Boolean = true): IO[List[LegalBase]] = {
        val qNoPS =
          sql"""
            select id, type, name, description, active
            from legal_bases
            where appid = $appId
          """
            .query[(UUID, LegalBaseTerms, Option[String], Option[String], Boolean)]
            .map {
              case (id, lbType, name, desc, active) =>
                LegalBase(id, lbType, PrivacyScope.empty, name, desc, active)
            }

        val qWithPS =
          sql"""
            select lb.id as id, lb.type as type, lb.name as name, lb.description as description, lb.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
            from legal_bases lb
              join legal_bases_scope lbs on lbs.lbid = lb.id
              join "scope" s on s.id = lbs.scid
              join data_categories dc on dc.id = s.dcid
              join processing_categories pc on pc.id = s.pcid
              join processing_purposes pp on pp.id = s.ppid
            where lb.appid = $appId
            group by lb.id
          """
            .query[LegalBase]

        val res = if scope then qWithPS else qNoPS

        res.to[List].transact(xa)
      }

      def get(appId: UUID, ds: DataSubject): IO[List[LegalBase]] = ???

      def get(appId: UUID, lbId: UUID, scope: Boolean): IO[Option[LegalBase]] = {
        val qNoPS =
          sql"""
            select id, type, name, description, active
            from legal_bases
            where appid = $appId and id = $lbId
          """
            .query[(UUID, LegalBaseTerms, Option[String], Option[String], Boolean)]
            .map {
              case (id, lbType, name, desc, active) =>
                LegalBase(id, lbType, PrivacyScope.empty, name, desc, active)
            }

        val qWithPS =
          sql"""
            select lb.id as id, lb.type as type, lb.name as name, lb.description as description, lb.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
            from legal_bases lb
              join legal_bases_scope lbs on lbs.lbid = lb.id
              join "scope" s on s.id = lbs.scid
              join data_categories dc on dc.id = s.dcid
              join processing_categories pc on pc.id = s.pcid
              join processing_purposes pp on pp.id = s.ppid
            where lb.appid = $appId and lb.id = $lbId
            group by lb.id
          """
            .query[LegalBase]

        val res = if scope then qWithPS else qNoPS

        res.option.transact(xa)
      }

      def get(appId: UUID, lbId: NonEmptyList[UUID]): IO[List[LegalBase]] = {
        val q =
          sql"""
            select id, type, name, description, active
            from legal_bases
            where appid = $appId and
          """ ++ Fragments.in(fr"id", lbId)

        q.query[(UUID, LegalBaseTerms, Option[String], Option[String], Boolean)]
          .map {
            case (id, lbType, name, desc, active) =>
              LegalBase(id, lbType, PrivacyScope.empty, name, desc, active)
          }
          .to[List]
          .transact(xa)
      }

      def getByScope(appId: UUID, scope: PrivacyScope): IO[Option[UUID]] = {
        val s =
          scope.triples
            .map(t => s"${t.dataCategory.term} ${t.processingCategory.term} ${t.purpose.term}")
            .toList

        // TODO: this is probably inefficient. optimize or find a better way
        val q = sql"""
          select lb.id from legal_bases lb
            join legal_bases_scope lbs on lbs.lbid = lb.id
            join scope s on s.id = lbs.scid
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
          where lb.appid = $appId and lb.proactive = true
          group by lb.id
          having (SELECT ARRAY(SELECT unnest(array_agg(concat(dc.term, ' ', pc.term, ' ', pp.term))::varchar[]) ORDER BY 1)) =
            (SELECT ARRAY(SELECT unnest($s::varchar[]) order by 1))
        """

        q.query[UUID].option.transact(xa)
      }

      def add(appId: UUID, lb: LegalBase, proactive: Boolean = false): IO[Unit] =
        sql"""
          insert into legal_bases (id, appid, type, name, description, active, proactive)
          values (${lb.id}, $appId, ${lb.lbType.encode}::legal_base_terms, ${lb.name}, ${lb.description}, ${lb.active}, $proactive)
        """.update.run.transact(xa).void

      def addScope(appId: UUID, lbId: UUID, scope: PrivacyScope): IO[Unit] = {
        // this can be slow for big scopes so shifting
        // to a different pool not to block the main one
        val insertLbScope =
          IO {
            fr"""
              insert into legal_bases_scope
                select $lbId, s.id
                from "scope" s
                join data_categories dc ON dc.id = s.dcid
                join processing_categories pc ON pc.id = s.pcid
                join processing_purposes pp ON pp.id = s.ppid
                where (dc.appid is null or dc.appid=$appId) and
            """ ++ Fragments.or(
              scope.triples
                .map(
                  t =>
                    fr"dc.term = ${t.dataCategory.term} and" ++
                      fr"pc.term = ${t.processingCategory.term} and" ++
                      fr"pp.term = ${t.purpose.term}"
                )
                .toList*
            )
          }.evalOn(pools.cpu)

        insertLbScope.flatMap(_.update.run.transact(xa).void)
      }

    }

}
