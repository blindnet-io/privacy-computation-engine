package io.blindnet.pce

import cats.data.*
import cats.effect.*
import cats.implicits.*
import ch.qos.logback.core.net.server.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import db.*
import db.repositories.*
import requesthandlers.*
import api.*
import services.*
import config.{ *, given }
import io.blindnet.identityclient.IdentityClientBuilder
import io.blindnet.identityclient.auth.JwtAuthenticator
import services.external.StorageInterface
import dev.profunktor.redis4cats.*
import dev.profunktor.redis4cats.effect.Log.Stdout.*

object Main extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val app = for {
      _    <- Resource.eval(logger.info(s"Starting app. \n ${build.BuildInfo.toString}"))
      conf <- Resource.eval(Config.load)
      _    <- Resource.eval(logger.info(show"$conf"))
      xa   <- DbTransactor.make(conf.db)
      _    <- Resource.eval(xa.configure(Migrator.migrateDatabase))

      redis <- Redis[IO].utf8("redis://localhost")

      cpuPool <- Pools.cpu
      pools = Pools(cpuPool)

      repositories = Repositories.live(xa, redis, pools)

      httpClient <- EmberClientBuilder.default[IO].build
      storage = StorageInterface.live(httpClient, repositories, conf)

      services = Services.make(repositories, conf)

      _ <- Resource.eval(RequestHandlers.run(repositories, storage)).start

      identityClient <- IdentityClientBuilder().withClient(httpClient).resource

      app = AppRouter.make(services, JwtAuthenticator(identityClient))
      server <- Server.make(app.httpApp, conf.api)

    } yield ()

    app.useForever.onError(e => logger.error(e)("Unrecoverable error occurred. Shutting down"))

  }

}
