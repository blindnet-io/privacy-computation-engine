package io.blindnet.pce
package requesthandlers

import cats.effect.*
import cats.effect.std.*
import requesthandlers.recommender.*
import requesthandlers.calculator.*
import db.repositories.Repositories
import io.blindnet.pce.clients.StorageClient

object RequestHandlers {
  // TODO: resource
  def run(
      repos: Repositories,
      storage: StorageClient
  ): IO[Unit] = {

    for {
      _ <- Recommender.run(repos).start
      _ <- ResponseCalculator.run(repos).start
      _ <- StorageCommandsHandler.run(repos, storage).start
    } yield ()
  }

}
