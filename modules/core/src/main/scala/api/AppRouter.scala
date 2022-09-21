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
import services.Services
import sttp.apispec.Tag
import sttp.apispec.ExternalDocumentation

object AppRouter {
  def make(services: Services) = new AppRouter(services)
}

class AppRouter(services: Services) {

  val healthCheckEndpoints       = new HealthCheckEndpoints()
  val privacyRequestEndpoints    = new PrivacyRequestEndpoints(services.privacyRequest)
  val consumerInterfaceEndpoints = new DataConsumerEndpoints(services.consumerInterface)
  val configurationEndpoints     = new ConfigurationEndpoints(services.configuration)
  val userEventsEndpoints        = new UserEventsEndpoints(services.userEvents)
  val userEndpoints              = new UserEndpoints(services.user)
  val callbackEndpoints          = new CallbackEndpoints(services.callbacks)

  val allEndpoints =
    healthCheckEndpoints.endpoints ++
      privacyRequestEndpoints.endpoints ++
      consumerInterfaceEndpoints.endpoints ++
      configurationEndpoints.endpoints ++
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

  val httpApp = middleware(routes).orNotFound

}
