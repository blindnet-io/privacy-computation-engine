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

object AppRouter {
  def make(services: Services) = new AppRouter(services)
}

class AppRouter(services: Services) {

  val healthCheckEndpoints = new HealthCheckEndpoints().endpoints

  val privacyRequestEndpoints    = new PrivacyRequestEndpoints(services.privacyRequest).endpoints
  val consumerInterfaceEndpoints = new DataConsumerEndpoints(services.consumerInterface).endpoints
  // data subject endpoints
  // customization endpoints

  val allEndpoints = healthCheckEndpoints ++ privacyRequestEndpoints ++ consumerInterfaceEndpoints

  // val docs: OpenAPI =
  //   OpenAPIDocsInterpreter()
  //     .serverEndpointsToOpenAPI(
  //       documentedEndpoints,
  //       "Privacy computation engine",
  //       build.BuildInfo.version
  //     )

  // println(docs.toYaml)

  val swagger =
    SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("swagger")))
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
