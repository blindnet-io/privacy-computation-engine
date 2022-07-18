package io.blindnet.privacy
package api

import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.*
import cats.effect.*

import endpoints.*

object App {
  def make() = new App()
}

class App() {

  val healthCheckEndpoints = new HealthCheckEndpoints().routes

  val routes: HttpRoutes[IO] = Router(
    "v0" -> healthCheckEndpoints
  )

  private val middleware: HttpRoutes[IO] => HttpRoutes[IO] = {
    { (routes: HttpRoutes[IO]) => AutoSlash(routes) }
      .andThen { routes => CORS.policy.withAllowOriginAll(routes) }
  }

  val httpApp = middleware(routes).orNotFound
}
