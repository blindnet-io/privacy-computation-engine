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
import io.blindnet.pce.priv.privacyrequest.ResponseEventId
import io.blindnet.pce.priv.terms.Action

class CallbackHandler(repos: Repositories) {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  // TODO: error handling
  def handle(cbId: UUID, req: DataCallbackPayload): IO[Unit] =
    for {
      _      <- logger.info(s"Received callback for id $cbId. req:\n${req.asJson}")
      cbData <- repos.callbacks.get(cbId).orFail(s"Wrong callback id ${cbId}")
      _      <- "Wrong app id".failBadRequest.whenA(req.app_id != cbData.aid)
      preId = ResponseEventId(cbData.rid)
      _ <- repos.callbacks.remove(cbId)

      d <- repos.privacyRequest.getDemandFromResponseEvent(preId).orNotFound("Demand not found")

      // TODO: messages and localization
      _ <- (req.accepted, d.action) match {
        case (true, Action.Access) =>
          repos.privacyRequest.storeResponseData(preId, req.data_url)
        case (true, Action.Delete) =>
          repos.privacyRequest.storeResponseData(preId, Some("Data successfully deleted"))
        case (true, _)             =>
          repos.privacyRequest.storeResponseData(preId, None)
        case (false, _)            =>
          repos.privacyRequest.storeResponseData(
            preId,
            Some("Storage rejected the requested action")
          )
      }

    } yield ()

}
