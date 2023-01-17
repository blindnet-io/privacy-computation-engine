package io.blindnet.pce
package api

import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.*
import sttp.apispec.openapi.OpenAPI
import sttp.apispec.openapi.circe.yaml.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.*
import sttp.tapir.docs.openapi.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import sttp.tapir.swagger.*
import sttp.tapir.swagger.bundle.*
import endpoints.*
import io.blindnet.identityclient.auth.*
import services.Services
import sttp.apispec.Tag
import sttp.apispec.ExternalDocumentation
import io.blindnet.pce.db.repositories.Repositories
import io.blindnet.pce.config.Config
import io.blindnet.identityclient.IdentityClient

object AppRouter {
  def make(
      services: Services,
      repositories: Repositories,
      identityClient: IdentityClient,
      config: Config
  ) =
    new AppRouter(services, repositories, identityClient, config)

}

class AppRouter(
    services: Services,
    repositories: Repositories,
    identityClient: IdentityClient,
    config: Config
) {

  val jwtAuthenticator           = JwtAuthenticator(identityClient)
  val identityJwtAuthenticator   = JwtLocalAuthenticator(config.components.identityKey)
  val identityConstAuthenticator = ConstAuthenticator(config.tokens.identity.value, IO.pure(()))

  val healthCheckEndpoints    = new HealthCheckEndpoints()
  val privacyRequestEndpoints =
    new PrivacyRequestEndpoints(jwtAuthenticator, services.privacyRequest)

  val bridgeEndpoints =
    new BridgeEndpoints(jwtAuthenticator, services.bridge)

  val configurationEndpoints =
    new ConfigurationEndpoints(jwtAuthenticator, identityJwtAuthenticator, services.configuration)

  val administrationEndpoints =
    new AdministrationEndpoints(identityConstAuthenticator, services.administration)

  val userEventsEndpoints = new UserEventsEndpoints(jwtAuthenticator, services.userEvents)
  val userEndpoints       = new UserEndpoints(jwtAuthenticator, services.user)
  val callbackEndpoints   = new CallbackEndpoints(services.callbacks)

  val allEndpoints =
    healthCheckEndpoints.endpoints ++
      privacyRequestEndpoints.endpoints ++
      bridgeEndpoints.endpoints ++
      configurationEndpoints.endpoints ++
      administrationEndpoints.endpoints ++
      userEventsEndpoints.endpoints ++
      userEndpoints.endpoints ++
      callbackEndpoints.endpoints

  // val docs: OpenAPI =
  //   OpenAPIDocsInterpreter()
  //     .serverEndpointsToOpenAPI(
  //       documentedEndpoints,
  //       "Privacy computation engine",
  //       build.BuildInfo.version
  //     )

  // println(docs.toYaml)

  val tags = List(
    Tag(
      configurationEndpoints.Tag,
      externalDocs = Some(ExternalDocumentation(configurationEndpoints.DocsUri, Some("docs")))
    )
  )

  val swagger =
    SwaggerInterpreter(
      swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("swagger")),
      customiseDocsModel = _.tags(tags)
    )
      .fromServerEndpoints[IO](
        allEndpoints,
        "Privacy computation engine",
        build.BuildInfo.version
      )

  private val http4sOptions = Http4sServerOptions
    .customiseInterceptors[IO]
    .exceptionHandler(None)
    .serverLog(None)
    .options

  val allRoutes =
    Http4sServerInterpreter[IO](http4sOptions).toRoutes(allEndpoints) <+>
      Http4sServerInterpreter[IO]().toRoutes(swagger)

  val routes: HttpRoutes[IO] = allRoutes

  private val middleware: HttpRoutes[IO] => HttpRoutes[IO] = {
    { (routes: HttpRoutes[IO]) => AutoSlash(routes) }
      .andThen { routes => CORS.policy.withAllowOriginAll(routes) }
      .andThen { routes => ErrorHandlerMiddleware.apply(routes) }
  }

  val httpApp = Router("/v0" -> middleware(routes)).orNotFound

}
