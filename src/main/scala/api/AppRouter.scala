package io.blindnet.privacy
package api

import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.*
import endpoints.*
import services.Services

object AppRouter {
  def make(services: Services) = new AppRouter(services)
}

class AppRouter(services: Services) {

  val healthCheckEndpoints    = new HealthCheckEndpoints().routes
  val privacyRequestEndpoints = new PrivacyRequestEndpoints(services.privacyRequest).routes
  // data subject endpoints
  // data consumer endpoints
  // customization endpoints

  // TODO: token
  val protectedRoutes = privacyRequestEndpoints

  val allRoutes = healthCheckEndpoints <+> protectedRoutes

  val routes: HttpRoutes[IO] = Router(
    "v0" -> allRoutes
  )

  private val middleware: HttpRoutes[IO] => HttpRoutes[IO] = {
    { (routes: HttpRoutes[IO]) => AutoSlash(routes) }
      .andThen { routes => CORS.policy.withAllowOriginAll(routes) }
      .andThen { routes => ErrorHandlerMiddleware.apply(routes) }
  }

  val httpApp = middleware(routes).orNotFound
}
