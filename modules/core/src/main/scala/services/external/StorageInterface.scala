package io.blindnet.pce
package services.external

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
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

// TODO: refactor
trait StorageInterface {
  def requestAccessLink(
      callbackId: UUID,
      appId: UUID,
      demandId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

  def requestDeletion(
      callbackId: UUID,
      appId: UUID,
      demandId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

}

object StorageInterface {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def live(c: Client[IO], repos: Repositories, conf: Config) =
    new StorageInterface {
      def requestAccessLink(
          callbackId: UUID,
          appId: UUID,
          demandId: UUID,
          subject: DataSubject,
          rec: Recommendation
      ): IO[Unit] = {

        val payload = DataRequestPayload(
          request_id = demandId.toString(),
          DataQueryPayload(
            // selectors = rec.dataCategories.map(_.term).toList,
            // subjects = List(subject.id),
            selectors = List("CONTACT.EMAIL", "NAME", "OTHER-DATA.PROOF"),
            subjects = List("john.doe@example.com"),
            provenance = rec.provenance.map(_.encode),
            target = None,
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          action = DataRequestAction.GET,
          callback = (conf.callbackUri / "callback" / callbackId.toString).toString
        )

        def req(uri: Uri) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests"
        )
          .withEntity(payload)

        for {
          // TODO: handle not found
          app <- repos.app.get(appId).map(_.get)
          _   <- logger.info(s"Sending request to ${app.dacUri} \n ${payload.asJson}")
          res <- c.successful(req(app.dacUri))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

      def requestDeletion(
          callbackId: UUID,
          appId: UUID,
          demandId: UUID,
          subject: DataSubject,
          rec: Recommendation
      ): IO[Unit] = {
        val payload = DataRequestPayload(
          request_id = demandId.toString(),
          DataQueryPayload(
            selectors = rec.dataCategories.map(_.term).toList,
            subjects = List(subject.id),
            provenance = rec.provenance.map(_.encode),
            target = None,
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          action = DataRequestAction.DELETE,
          callback = (conf.callbackUri / "callback" / callbackId.toString).toString
        )

        def req(uri: Uri) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests"
        )
          .withEntity(payload)

        for {
          // TODO: handle not found
          app <- repos.app.get(appId).map(_.get)
          _   <- logger.info(s"Sending request to ${app.dacUri} \n ${payload.asJson}")
          res <- c.successful(req(app.dacUri))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

    }

}
