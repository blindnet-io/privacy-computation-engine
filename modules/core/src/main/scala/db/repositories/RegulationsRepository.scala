package io.blindnet.pce
package db.repositories

import java.util.UUID
import javax.xml.crypto.Data

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

trait RegulationsRepository {

  def get(appId: UUID, ctx: PSContext): IO[List[Regulation]]

  def getInfo(): IO[List[RegulationInfo]]

  def getInfo(appId: UUID): IO[List[RegulationInfo]]

  def exists(ids: NonEmptyList[UUID]): IO[Boolean]

  def add(appId: UUID, ids: NonEmptyList[UUID]): IO[Unit]

  def delete(appId: UUID, id: UUID): IO[Unit]
}

object RegulationsRepository {
  def live(xa: Transactor[IO]): RegulationsRepository =
    new RegulationsRepository {

      def get(appId: UUID, ctx: PSContext): IO[List[Regulation]] =
        sql"""
          select ar.rid as rid, rlbfs.legal_base as lb, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
          from app_regulations ar
            join regulation_legal_base_forbidden_scope rlbfs on rlbfs.rid = ar.rid
            join scope s on s.id = rlbfs.scid
              join data_categories dc on dc.id = s.dcid
              join processing_categories pc on pc.id = s.pcid
              join processing_purposes pp on pp.id = s.ppid
          where ar.appid = $appId
          group by ar.rid, rlbfs.legal_base
        """
          .query[(UUID, LegalBaseTerms, List[String], List[String], List[String])]
          .to[List]
          .map(_.groupBy(_._1))
          .map(_.map {
            case (id, l) =>
              Regulation(
                id,
                l.map {
                  case (_, lb, dcs, pcs, pps) =>
                    lb -> PrivacyScope.unsafe(dcs, pcs, pps).zoomIn(ctx)
                }.toMap
              )
          }.toList)
          .transact(xa)

      def getInfo(): IO[List[RegulationInfo]] =
        sql"""
          select id, name, description
          from regulations
        """
          .query[RegulationInfo]
          .to[List]
          .transact(xa)

      def getInfo(appId: UUID): IO[List[RegulationInfo]] =
        sql"""
          select ar.rid as rid, r.name as name, r.description as description
          from app_regulations ar
            join regulations r on r.id = ar.rid
          where ar.appid = $appId
        """
          .query[RegulationInfo]
          .to[List]
          .transact(xa)

      def exists(ids: NonEmptyList[UUID]): IO[Boolean] =
        (fr"select count(*) from regulations where" ++ Fragments.in(fr"id", ids))
          .query[Int]
          .map(_ == ids.length)
          .unique
          .transact(xa)

      def add(appId: UUID, ids: NonEmptyList[UUID]): IO[Unit] =
        val sql = s"""
            insert into app_regulations
            values ('$appId', ?)
          """
        Update[UUID](sql).updateMany(ids).transact(xa).void

      def delete(appId: UUID, id: UUID): IO[Unit] =
        sql"""delete from app_regulations where appid = $appId and rid = $id""".update.run
          .transact(xa)
          .void

    }

}
