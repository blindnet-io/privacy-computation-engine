package io.blindnet.privacy
package api.endpoints

import cats.effect.IO
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.server.Router
import services.*
import api.endpoints.payload.{ given, * }

class PrivacyRequestEndpoints(
    reqService: PrivacyRequestService
) extends Http4sDsl[IO] {

  val appId = "6f083c15-4ada-4671-a6d1-c671bc9105dc"

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case r @ POST -> Root / "privacy-request" =>
      for {
        // TODO: validate token and get appId
        req  <- r.as[PrivacyRequestPayload]
        res  <- reqService.processRequest(req, appId)
        resp <- Ok(res)
      } yield resp
  }

}
