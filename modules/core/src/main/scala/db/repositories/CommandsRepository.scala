package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.pce.db.DbUtil
import io.blindnet.pce.util.extension.*
import priv.*
import priv.terms.*
import io.blindnet.pce.model.*
import doobie.free.connection

trait CommandsRepository {
  def getCreateRec(n: Int = 0): IO[List[CommandCreateRecommendation]]

  def addCreateRec(cs: List[CommandCreateRecommendation]): IO[Unit]

  def getCreateResp(n: Int = 0): IO[List[CommandCreateResponse]]

  def addCreateResp(cs: List[CommandCreateResponse]): IO[Unit]
}

object CommandsRepository {
  def live(xa: Transactor[IO]): CommandsRepository =
    new CommandsRepository {

      def getCreateRec(n: Int = 0): IO[List[CommandCreateRecommendation]] = {
        val select = sql"""
          select id, did, date, data
          from commands_create_recommendation
          order by date asc
          limit $n
        """.query[CommandCreateRecommendation].to[List]

        val delete = (ids: NonEmptyList[UUID]) => (fr"""
          delete from commands_create_recommendation
          where
        """ ++ Fragments.in(fr"id", ids)).update.run

        val p = for {
          ccrs <- select
          _    <- NonEmptyList.fromList(ccrs.map(_.id)) match {
            case Some(ids) => delete(ids)
            case None      => connection.unit
          }
        } yield ccrs

        p.transact(xa)
      }

      def addCreateRec(cs: List[CommandCreateRecommendation]): IO[Unit] =
        val sql = """
            insert into commands_create_recommendation
            values (?, ?, ?, ?)
          """
        Update[CommandCreateRecommendation](sql).updateMany(cs).transact(xa).void

      def getCreateResp(n: Int = 0): IO[List[CommandCreateResponse]] = {
        val select = sql"""
          select id, did, date, data
          from commands_create_response
          order by date asc
          limit $n
        """.query[CommandCreateResponse].to[List]

        val delete = (ids: NonEmptyList[UUID]) => (fr"""
          delete from commands_create_response
          where
        """ ++ Fragments.in(fr"id", ids)).update.run

        val p = for {
          ccrs <- select
          _    <- NonEmptyList.fromList(ccrs.map(_.id)) match {
            case Some(ids) => delete(ids)
            case None      => connection.unit
          }
        } yield ccrs

        p.transact(xa)
      }

      def addCreateResp(cs: List[CommandCreateResponse]): IO[Unit] =
        val sql = """
            insert into commands_create_response
            values (?, ?, ?, ?)
          """
        Update[CommandCreateResponse](sql).updateMany(cs).transact(xa).void

    }

}
