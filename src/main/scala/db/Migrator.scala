package io.blindnet.privacy
package db

import cats.effect.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import db.DbConfig

object Migrator {

  def migrateDatabase(conf: DbConfig): IO[MigrateResult] = {

    val flywayConf = Flyway
      .configure()
      .dataSource(conf.uri, conf.username, conf.password)
      // .group(true)
      .table("Flyway")
      .locations(org.flywaydb.core.api.Location("classpath:migrations"))
      .baselineOnMigrate(true)

    for {
      // TODO: logger
      _   <- IO(println(flywayConf.load().validateWithResult().getAllErrorMessages()))
      res <- IO(flywayConf.load().migrate())
    } yield res

  }

}
