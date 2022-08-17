package io.blindnet.pce

import cats.data.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import db.*
import db.repositories.*
import tasks.*
import api.*
import services.*
import config.{ given, * }
import org.http4s.ember.client.EmberClientBuilder
import ch.qos.logback.core.net.server.Client
import services.external.StorageInterface

object Main extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val app = for {
      _    <- Resource.eval(logger.info(s"Starting app. \n ${build.BuildInfo.toString}"))
      conf <- Resource.eval(Config.load)
      _    <- Resource.eval(logger.info(show"$conf"))
      _    <- Resource.eval(Migrator.migrateDatabase(conf.db))
      xa   <- DbTransactor.make(conf.db)

      repositories <- Resource.eval(Repositories.live(xa))

      httpClient <- EmberClientBuilder.default[IO].build
      storage = StorageInterface.live(httpClient, repositories, conf)

      services = Services.make(repositories, conf)

      _ <- Resource.eval(Tasks.run(repositories, storage).start)

      app = AppRouter.make(services)
      server <- Server.make(app.httpApp, conf.api)

    } yield ()

    app.useForever.onError(e => logger.error(e)("Unrecoverable error occurred. Shutting down"))

  }

}
