package io.blindnet.pce
package services

import java.util.UUID

import cats.effect.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.*
import org.http4s.client.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.*
import priv.*
import config.Config
import model.error.InternalException
import io.blindnet.pce.db.repositories.Repositories
import api.endpoints.messages.callback.*
import io.blindnet.pce.util.extension.*
import cats.effect.std.UUIDGen
import io.blindnet.pce.services.util.failBadRequest

class CallbackService(repos: Repositories) {

  def handle(appId: UUID, cbId: UUID, req: DataCallbackPayload): IO[Unit] =
    for {
      cbData <- repos.callbacks.get(cbId).orFail(s"Wrong id ${cbId}")
      (appId2, rId) = cbData
      _ <-
        if appId == appId2
        then IO.unit
        // TODO: msg
        else "Error".failBadRequest

      _ <- repos.privacyRequest.storeResponseData(rId, req.data_url)
      _ <- repos.callbacks.remove(cbId)
    } yield ()

}
