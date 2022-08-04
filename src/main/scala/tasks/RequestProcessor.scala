package io.blindnet.privacy
package tasks

import cats.effect.*
import db.repositories.Repositories
import state.State
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

class RequestProcessor(
    repos: Repositories
) {

  def processRequest(reqId: String): IO[Unit] = {
    IO.unit
  }

  def processDemand(demandId: String) = {}

}

object RequestProcessor {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories, state: State): IO[Unit] = {
    val reqProc = new RequestProcessor(repos)

    def loop(): IO[Unit] =
      for {
        reqId <- state.pendingRequests.take
        _     <- logger.info(s"Processing new request $reqId")
        // TODO: handle errors - repeat
        f     <- reqProc
          .processRequest(reqId)
          .flatMap(_ => logger.info(s"Request $reqId processed"))
          .start
        _     <- loop()
      } yield ()

    loop()
  }

}
