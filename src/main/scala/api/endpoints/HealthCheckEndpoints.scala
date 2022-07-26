package io.blindnet.privacy
package api.endpoints

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl

class HealthCheckEndpoints() extends Http4sDsl[IO] {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" => Ok()
  }

}
