package io.blindnet.pce
package db

import cats.effect.*
import io.blindnet.pce.model.error.MigrationError
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import config.DbConfig
import javax.sql.DataSource

object Migrator {

  def migrateDatabase(ds: DataSource): IO[Unit] = {

    val flywayConf = Flyway
      .configure()
      .dataSource(ds)
      // .group(true)
      .table("Flyway")
      .locations(org.flywaydb.core.api.Location("classpath:db/migration"))
      .baselineOnMigrate(true)
      .ignorePendingMigrations(true)

    for {
      validation <- IO(flywayConf.load().validateWithResult())

      _ <-
        if validation.validationSuccessful then IO.unit
        else IO.raiseError(MigrationError(validation.getAllErrorMessages()))

      res <- IO(flywayConf.load().migrate())

      _ <-
        if res.success then IO.unit
        else IO.raiseError(MigrationError("Migration failed"))

    } yield ()

  }

}
