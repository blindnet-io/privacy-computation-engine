package io.blindnet.pce
package api.endpoints

import cats.effect.*
import sttp.tapir.*
import sttp.tapir.server.http4s.*

class HealthCheckEndpoints() {
  val base = util.baseEndpoint.tag("Health")

  val health =
    base
      .description("Is the app running?")
      .get
      .in("health")
      .serverLogicSuccess(_ => IO.unit)

  val endpoints = List(health)

}
