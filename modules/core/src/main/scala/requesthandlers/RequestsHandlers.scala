package io.blindnet.pce
package requesthandlers

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories
import services.external.StorageInterface

object RequestHandlers {
  // TODO: resource
  def run(
      repos: Repositories,
      storage: StorageInterface
  ): IO[Unit] = {

    for {
      _ <- RequestProcessor.run(repos).start
      _ <- RequestResponder.run(repos, storage).start
    } yield ()
  }

}
