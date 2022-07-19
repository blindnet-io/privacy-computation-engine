package io.blindnet.privacy
package api

import cats.effect.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.*
import endpoints.*

object AppRouter {
  def make() = new AppRouter()
}

class AppRouter() {

  val healthCheckEndpoints = new HealthCheckEndpoints().routes
  // data subject endpoints
  // data consumer endpoints
  // customization endpoints

  val routes: HttpRoutes[IO] = Router(
    "v0" -> healthCheckEndpoints
  )

  private val middleware: HttpRoutes[IO] => HttpRoutes[IO] = {
    { (routes: HttpRoutes[IO]) => AutoSlash(routes) }
      .andThen { routes => CORS.policy.withAllowOriginAll(routes) }
  }

  val httpApp = middleware(routes).orNotFound
}
