package io.blindnet.pce
package requesthandlers.calculator

import java.util.UUID

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.*
import io.blindnet.pce.priv.DataSubject
import io.blindnet.pce.services.external.StorageInterface
import io.blindnet.pce.util.extension.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*
import priv.Recommendation
import priv.privacyrequest.*
import priv.terms.*
import db.repositories.Repositories
import io.blindnet.pce.db.repositories.CBData

class StorageCommandsHandler(
    repos: Repositories,
    storage: StorageInterface
) {

  import priv.terms.Action.*
  import priv.terms.Status.*

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val transparency = TransparencyCalculator(repos)
  val general      = GeneralCalculator(repos)

  private def handle(cis: CommandInvokeStorage): IO[Unit] =
    for {
      pr  <- repos.privacyRequest.getRequestFromDemand(cis.dId).map(_.get)
      r   <- repos.privacyRequest.getRecommendation(cis.dId).map(_.get)
      app <- repos.app.get(pr.appId).map(_.get)
      _   <- callStorage(app, cis, pr.dataSubject, r)
    } yield ()

  private def callStorage(
      app: PCEApp,
      cis: CommandInvokeStorage,
      ds: Option[DataSubject],
      r: Recommendation
  ) =
    (cis.action, ds) match {
      case (StorageAction.Get, Some(ds))    =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, CBData(app.id, cis.preId))
          _    <- storage.get(app, cbId, cis.dId, ds, r)
        } yield ()
      case (StorageAction.Delete, Some(ds)) =>
        for {
          cbId <- UUIDGen.randomUUID[IO]
          _    <- repos.callbacks.set(cbId, CBData(app.id, cis.preId))
          _    <- storage.delete(app, cbId, cis.dId, ds, r)
        } yield ()
      case _                                => IO.unit
    }

}

object StorageCommandsHandler {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(repos: Repositories, storage: StorageInterface): IO[Unit] = {
    val storageHandler = new StorageCommandsHandler(repos, storage)

    def loop(): IO[Unit] =
      for {
        cs <- repos.commands.popInvokeStorage(5)
        _  <- cs.parTraverse_(
          c => {
            val preId = c.preId
            val p     = for {
              _ <- logger.info(s"Calling storage for response event $preId")
              _ <- storageHandler.handle(c)
            } yield ()

            p.handleErrorWith(
              e =>
                logger
                  .error(e)(s"Error calling storage for response event $preId\n${e.getMessage}")
                  .flatMap(_ => repos.commands.pushInvokeStorage(List(c.addRetry)))
            )
          }
        )

        _ <- IO.sleep(5.second)
        _ <- loop()
      } yield ()

    loop()
  }

}
