package io.blindnet.pce

import java.time.Instant
import java.util.UUID
import weaver.*
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, PostgreSQLContainer }
import org.testcontainers.utility.DockerImageName
import doobie.util.transactor.Transactor
import cats.effect.{ Resource, Sync }
import cats.syntax.all.*
import cats.effect.IO
import org.http4s.client.*
import org.http4s.ember.client.EmberClientBuilder
import io.blindnet.pce.db.repositories.Repositories
import io.blindnet.pce.db.Migrator

import javax.sql.DataSource
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import fs2.text
import fs2.io.file.{ Files, Path }
import io.blindnet.pce.services.Services
import io.blindnet.pce.config.*
import org.http4s.*
import io.blindnet.pce.priv.DataSubject
import com.comcast.ip4s.*
import io.blindnet.pce.api.*
import cats.data.Kleisli
import io.blindnet.identityclient.IdentityClientBuilder
import io.blindnet.identityclient.auth.*
import io.circe.literal.*
import org.http4s.circe.*
import io.blindnet.identityclient.IdentityClient
import io.blindnet.jwt.*
import org.testcontainers.containers.GenericContainer
import dev.profunktor.redis4cats.*
import dev.profunktor.redis4cats.effect.Log.Stdout.*
import testutil.*
import httputil.*

object SharedResources extends GlobalResource {

  def populateDb(xa: Transactor[IO]) =
    for {
      sql <- Files[IO]
        .readAll(Path("modules/core/src/test/resources/db/insert.sql"))
        .through(text.utf8.decode)
        .compile
        .fold("")(_ + _)
      _   <- Update[Unit](sql).run(()).transact(xa).void
    } yield ()

  case class Resources(
      xa: Transactor[IO],
      client: Client[IO],
      repos: Repositories,
      services: Services,
      server: Kleisli[IO, Request[IO], Response[IO]]
  )

  // TODO: move to identity client library
  val identityHttpClient = Client[IO] {
    req =>
      val resp = req.uri.renderString match {
        case s"$_/applications/$id" =>
          Response[IO]().withEntity(
            json"""{ "id": $appId, "name": "test", "key": $publicKey }"""
          )

        case _ =>
          Response[IO](status = Status.BadRequest)
      }

      Resource.make(IO(resp))(_ => IO.unit)
  }

  val baseResources: Resource[IO, Resources] =
    for {
      pgContainer <- Resource.make {
        val container = PostgreSQLContainer(DockerImageName.parse("postgres:13"))
        IO.blocking(container.start()).as(container)
      }(c => IO.blocking(c.stop()))

      // redisContainer <- Resource.make {
      //   import scala.collection.JavaConverters.*
      //   val redisContainer = GenericContainer(DockerImageName.parse("redis:6.2.7-alpine"))
      //   redisContainer.setExposedPorts(List(Integer.valueOf(6379)).asJava)
      //   IO.blocking(redisContainer.start()).as(redisContainer)
      // }(c => IO.blocking(c.stop()))

      // format: off
      xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", pgContainer.jdbcUrl, pgContainer.username, pgContainer.password)
      _ <- Resource.eval(Migrator.migrateDatabase(pgContainer.jdbcUrl, pgContainer.username, pgContainer.password))
      // format: on
      _ <- Resource.eval(populateDb(xa))

      // redis = Redis[IO].utf8(s"redis://localhost:${redisContainer.getMappedPort(6379)}")

      client <- EmberClientBuilder.default[IO].build

      repos = Repositories.live(xa, null, Pools(scala.concurrent.ExecutionContext.global))

      conf     = Config(
        env = AppEnvironment.Development,
        callbackUri = Uri.unsafeFromString("localhost"),
        db = null,
        redis = null,
        api = ApiConfig(ipv4"0.0.0.0", port"9009"),
        components = ComponentsConfig()
      )
      services = Services.make(repos, conf)

      identityClient <- IdentityClientBuilder().withClient(identityHttpClient).resource

      server = AppRouter.make(services, repos, JwtAuthenticator(identityClient)).httpApp

      resources = Resources(xa, client, repos, services, server)

    } yield resources

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    baseResources.flatMap(global.putR(_))

  // Provides a fallback to support running individual tests via testOnly
  def sharedResourceOrFallback(read: GlobalRead): Resource[IO, Resources] =
    read.getR[Resources]().flatMap {
      case Some(value) => Resource.eval(IO(value))
      case None        => baseResources
    }

}
