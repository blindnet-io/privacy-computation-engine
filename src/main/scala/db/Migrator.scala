package io.blindnet.privacy
package db

import cats.effect.*
import io.blindnet.privacy.model.error.MigrationError
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import db.DbConfig

object Migrator {

  def migrateDatabase(conf: DbConfig): IO[Unit] = {

    val flywayConf = Flyway
      .configure()
      .dataSource(conf.uri, conf.username, conf.password)
      // .group(true)
      .table("Flyway")
      .locations(org.flywaydb.core.api.Location("classpath:migrations"))
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
