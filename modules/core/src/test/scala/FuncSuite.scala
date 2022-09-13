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
import fs2.{ text }
import fs2.io.file.{ Files, Path }
import io.blindnet.pce.services.Services
import io.blindnet.pce.config.*
import org.http4s.*
import io.blindnet.pce.priv.DataSubject
import com.comcast.ip4s.*
import io.blindnet.pce.api.*
import cats.data.Kleisli

trait FuncSuite extends IOSuite {

  val appId = UUID.fromString("6f083c15-4ada-4671-a6d1-c671bc9105dc")
  val ds    = DataSubject("fdfc95a6-8fd8-4581-91f7-b3d236a6a10e", appId)

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

  override type Res = Resources
  override def sharedResource: Resource[IO, Res] = {
    for {
      container <- Resource.make {
        val container = PostgreSQLContainer(DockerImageName.parse("postgres:13"))
        IO.blocking(container.start()).as(container)
      }(c => IO.blocking(c.stop()))
      
      // format: off
      xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", container.jdbcUrl, container.username, container.password)
      _ <- Resource.eval(Migrator.migrateDatabase(container.jdbcUrl, container.username, container.password))
      // format: on
      _ <- Resource.eval(populateDb(xa))

      client <- EmberClientBuilder.default[IO].build

      repos <- Resource.eval(Repositories.live(xa, Pools(scala.concurrent.ExecutionContext.global)))
      conf     = Config(
        env = AppEnvironment.Development,
        callbackUri = Uri.unsafeFromString("localhost"),
        db = null,
        api = ApiConfig(ipv4"0.0.0.0", port"9009"),
        components = ComponentsConfig()
      )
      services = Services.make(repos, conf)

      server = AppRouter.make(services).httpApp

    } yield Resources(xa, client, repos, services, server)
  }

}
