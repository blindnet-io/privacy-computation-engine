package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.free.connection
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.blindnet.pce.db.DbUtil
import io.blindnet.pce.model.*
import io.blindnet.pce.util.extension.*
import priv.*
import priv.terms.*

trait CommandsRepository {
  def popCreateRecommendation(n: Int = 0): IO[List[CommandCreateRecommendation]]

  def pushCreateRecommendation(cs: List[CommandCreateRecommendation]): IO[Unit]

  def popCreateResponse(n: Int = 0): IO[List[CommandCreateResponse]]

  def pushCreateResponse(cs: List[CommandCreateResponse]): IO[Unit]

  def popInvokeStorage(n: Int = 0): IO[List[CommandInvokeStorage]]

  def pushInvokeStorage(cs: List[CommandInvokeStorage]): IO[Unit]
}

object CommandsRepository {

  def live(xa: Transactor[IO]): CommandsRepository =
    new CommandsRepository {

      def popCreateRecommendation(n: Int = 0): IO[List[CommandCreateRecommendation]] = {
        val select = sql"""
          select id, did, date, data, retries
          from commands_create_recommendation
          where retries < 5
          order by date asc
          limit $n
        """.query[CommandCreateRecommendation].to[List]

        val delete = (ids: NonEmptyList[UUID]) => (fr"""
          delete from commands_create_recommendation
          where
        """ ++ Fragments.in(fr"id", ids)).update.run

        val p = for {
          cs <- select
          _  <- NonEmptyList.fromList(cs.map(_.id)) match {
            case Some(ids) => delete(ids)
            case None      => connection.unit
          }
        } yield cs

        p.transact(xa)
      }

      def pushCreateRecommendation(cs: List[CommandCreateRecommendation]): IO[Unit] =
        val sql = """
            insert into commands_create_recommendation
            values (?, ?, ?, ?, ?)
          """
        Update[CommandCreateRecommendation](sql).updateMany(cs).transact(xa).void

      def popCreateResponse(n: Int = 0): IO[List[CommandCreateResponse]] = {
        val select = sql"""
          select id, did, date, data, retries
          from commands_create_response
          where retries < 5
          order by date asc
          limit $n
        """.query[CommandCreateResponse].to[List]

        val delete = (ids: NonEmptyList[UUID]) => (fr"""
          delete from commands_create_response
          where
        """ ++ Fragments.in(fr"id", ids)).update.run

        val p = for {
          cs <- select
          _  <- NonEmptyList.fromList(cs.map(_.id)) match {
            case Some(ids) => delete(ids)
            case None      => connection.unit
          }
        } yield cs

        p.transact(xa)
      }

      def pushCreateResponse(cs: List[CommandCreateResponse]): IO[Unit] =
        val sql = """
            insert into commands_create_response
            values (?, ?, ?, ?, ?)
          """
        Update[CommandCreateResponse](sql).updateMany(cs).transact(xa).void

      def popInvokeStorage(n: Int = 0): IO[List[CommandInvokeStorage]] = {
        val select = sql"""
          select id, did, preid, action, data, date, retries
          from commands_invoke_storage
          where retries < 5
          order by date asc
          limit $n
        """.query[CommandInvokeStorage].to[List]

        val delete = (ids: NonEmptyList[UUID]) => (fr"""
          delete from commands_invoke_storage
          where
        """ ++ Fragments.in(fr"id", ids)).update.run

        val p = for {
          cs <- select
          _  <- NonEmptyList.fromList(cs.map(_.id)) match {
            case Some(ids) => delete(ids)
            case None      => connection.unit
          }
        } yield cs

        p.transact(xa)
      }

      def pushInvokeStorage(cs: List[CommandInvokeStorage]): IO[Unit] =
        val sql = """
            insert into commands_invoke_storage (id, did, preid, action, data, date, retries)
            values (?, ?, ?, ?::storage_action, ?, ?, ?)
          """
        Update[CommandInvokeStorage](sql).updateMany(cs).transact(xa).void

    }

}
