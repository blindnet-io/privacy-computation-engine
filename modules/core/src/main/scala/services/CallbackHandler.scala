package io.blindnet.pce
package services

import java.util.UUID

import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.db.repositories.Repositories
import io.blindnet.pce.util.extension.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.*
import org.http4s.client.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import priv.*
import config.Config
import model.error.InternalException
import api.endpoints.messages.callback.*

class CallbackHandler(repos: Repositories) {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  // TODO: handle errors
  def handleAccessResponse(appId: UUID, cbId: UUID, req: DataCallbackPayload): IO[Unit] =
    for {
      _      <- logger.info(s"Received callback for id $cbId. req:\n${req.asJson}")
      cbData <- repos.callbacks.get(cbId).orFail(s"Wrong callback id ${cbId}")
      _      <- repos.callbacks.remove(cbId)

      // TODO: msg
      _ <- "Error".failBadRequest.whenA(appId == cbData.aid)

      _ <- repos.privacyRequest.storeResponseData(cbData.rid, req.data_url)
    } yield ()

}
