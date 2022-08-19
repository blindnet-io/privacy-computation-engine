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

trait LegalBaseRepository {
  def get(appId: UUID, scope: Boolean = true): IO[List[LegalBase]]

  def get(appId: UUID, userIds: List[DataSubject]): IO[List[LegalBase]]

  def get(appId: UUID, lbId: UUID): IO[Option[LegalBase]]

  def add(appId: UUID, lb: LegalBase): IO[Unit]

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
          val scope = dcs.lazyZip(pcs).lazyZip(pps).map {
            case (dc, pc, pp) => PrivacyScopeTriple.unsafe(dc, pc, pp)
          }
          LegalBase(id, lbTerm, PrivacyScope(scope.toSet), name, desc, active)
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
            group by lb.id;
          """
            .query[LegalBase]

        val res = if scope then qWithPS else qNoPS

        res.to[List].transact(xa)
      }

      def get(appId: UUID, userIds: List[DataSubject]): IO[List[LegalBase]] = ???

      def get(appId: UUID, lbId: UUID): IO[Option[LegalBase]] = {
        sql"""
          select lb.id as id, lb.type as type, lb.name as name, lb.description as description, lb.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
          from legal_bases lb
            join legal_bases_scope lbs on lbs.lbid = lb.id
            join "scope" s on s.id = lbs.scid
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
          where lb.appid = $appId and lb.id = $lbId
          group by lb.id;
        """
          .query[LegalBase]
          .option
          .transact(xa)
      }

      def add(appId: UUID, lb: LegalBase): IO[Unit] = {

        val insertLb =
          sql"""
            insert into legal_bases (id, appid, type, name, description, active)
            values (${lb.id}, $appId, ${lb.lbType.encode}::legal_base_terms, ${lb.name}, ${lb.description}, ${lb.active})
          """

        // this can be slow for big scopes so shifting
        // to a different pool not to block the main one
        val insertLbScope =
          IO {
            fr"""
              insert into legal_bases_scope
                select ${lb.id}, s.id
                from "scope" s
                join data_categories dc ON dc.id = s.dcid
                join processing_categories pc ON pc.id = s.pcid
                join processing_purposes pp ON pp.id = s.ppid
                where
            """ ++ Fragments.or(
              lb.scope.triples
                .map(
                  t =>
                    fr"dc.term = ${t.dataCategory.term} and" ++
                      fr"pc.term = ${t.processingCategory.term} and" ++
                      fr"pp.term = ${t.purpose.term}"
                )
                .toList*
            )
          }.evalOn(pools.cpu)

        for {
          q <- insertLbScope
          _ <- (insertLb.update.run *> q.update.run).transact(xa)
        } yield ()

      }

    }

}
