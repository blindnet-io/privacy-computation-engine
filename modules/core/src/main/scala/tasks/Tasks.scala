package io.blindnet.pce
package tasks

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories
import tasks.RequestProcessor
import io.blindnet.pce.services.external.StorageInterface

object Tasks {
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
