package io.blindnet.pce
package db

import javax.sql.DataSource

import cats.effect.*
import io.blindnet.pce.model.error.MigrationError
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import config.DbConfig
import org.flywaydb.core.api.pattern.ValidatePattern

object Migrator {

  private def migrate(flywayConf: FluentConfiguration) =
    for {
      res <- IO(flywayConf.load().migrate())

      _ <-
        if res.success then IO.unit
        else IO.raiseError(MigrationError("Migration failed"))

    } yield ()

  def migrateDatabase(ds: DataSource): IO[Unit] = {

    val flywayConf = Flyway
      .configure()
      .dataSource(ds)
      // .group(true)
      .table("Flyway")
      .locations(org.flywaydb.core.api.Location("classpath:db/migration"))
      .baselineOnMigrate(true)

    migrate(flywayConf)
  }

  def migrateDatabase(uri: String, username: String, pass: String): IO[Unit] = {

    val flywayConf = Flyway
      .configure()
      .dataSource(uri, username, pass)
      // .group(true)
      .table("Flyway")
      .locations(org.flywaydb.core.api.Location("classpath:db/migration"))
      .baselineOnMigrate(true)
      .ignoreMigrationPatterns(ValidatePattern.fromPattern("*:pending"))

    migrate(flywayConf)

  }

}
