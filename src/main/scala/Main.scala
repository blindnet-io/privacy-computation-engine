package io.blindnet.privacy

import cats.data.*
import cats.effect.*
import cats.implicits.*
import io.blindnet.privacy.state.State
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import model.vocabulary.terms.Action
import db.*
import db.repositories.*
import tasks.*
import api.*
import services.*
import config.{ given, * }

object Main extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val app = for {
      _    <- Resource.eval(logger.info(s"Starting app. \n ${build.BuildInfo.toString}"))
      conf <- Resource.eval(Config.load)
      _    <- Resource.eval(logger.info(show"$conf"))
      _    <- Resource.eval(Migrator.migrateDatabase(conf.db))
      xa   <- DbTransactor.make(conf.db)

      state <- Resource.eval(State.make())
      repositories = Repositories.live(xa)
      services     = Services.make(repositories, state)

      _ <- Resource.eval(Tasks.run(repositories, state).start)

      app = AppRouter.make(services)
      server <- Server.make(app.httpApp, conf.api)

    } yield ()

    app.useForever.onError(e => logger.error(e)("Unrecoverable error occurred. Shutting down"))

  }

}
