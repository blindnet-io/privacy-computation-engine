package io.blindnet.privacy
package tasks

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories
import state.State
import tasks.RequestProcessor

object Tasks {
  def run(
      repos: Repositories,
      state: State
  ) = {

    for {
      _ <- RequestProcessor.run(repos, state).start
    } yield ()
  }

}
