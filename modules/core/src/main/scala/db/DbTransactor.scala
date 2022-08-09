package io.blindnet.pce
package db

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import config.DbConfig

object DbTransactor {

  def make(conf: DbConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor
        .newHikariTransactor[IO](
          "org.postgresql.Driver",
          conf.uri,
          conf.username,
          conf.password.value,
          ec
        )
    } yield xa

}
