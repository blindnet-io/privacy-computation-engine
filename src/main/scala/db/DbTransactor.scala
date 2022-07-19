package io.blindnet.privacy
package db

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import db.DbConfig

object DbTransactor {

  def make(conf: DbConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor
        .newHikariTransactor[IO](
          "org.postgresql.Driver",
          conf.uri,
          conf.username,
          conf.password,
          ec
        )
    } yield xa

}
