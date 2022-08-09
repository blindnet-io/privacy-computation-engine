package io.blindnet.pce
package tasks

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories
import tasks.RequestProcessor

object Tasks {
  def run(
      repos: Repositories
  ): IO[Unit] = {

    for {
      _ <- RequestProcessor.run(repos).start
    } yield ()
  }

}
