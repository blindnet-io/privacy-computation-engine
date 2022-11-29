package io.blindnet.pce
package services.external

import java.util.UUID

import cats.effect.*
import io.blindnet.pce.db.repositories.Repositories
import io.blindnet.pce.model.PCEApp
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

// TODO: refactor
trait StorageInterface {
  def get(
      app: PCEApp,
      callbackId: UUID,
      demandId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

  def delete(
      app: PCEApp,
      callbackId: UUID,
      demandId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

}

object StorageInterface {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def live(c: Client[IO], repos: Repositories, conf: Config) =
    new StorageInterface {
      def get(
          app: PCEApp,
          callbackId: UUID,
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
            target = rec.target.map(_.encode),
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          action = DataRequestAction.GET,
          callback = (conf.callbackUri / "callback" / callbackId.toString).toString
        )

        def req(uri: Uri, token: String) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests",
          headers = Headers("Authorization" -> s"Bearer $token")
        )
          .withEntity(payload)

        for {
          // TODO: exception
          uri   <- app.dac.uri.fold(IO.raiseError(new Exception("DAC uri not found")))(IO(_))
          token <- app.dac.token.fold(IO.raiseError(new Exception("DAC token not found")))(IO(_))
          _     <- logger.info(s"Sending request to $uri \n ${payload.asJson}")
          res   <- c.successful(req(uri, token))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

      def delete(
          app: PCEApp,
          callbackId: UUID,
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
            target = rec.target.map(_.encode),
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          action = DataRequestAction.DELETE,
          callback = (conf.callbackUri / "callback" / callbackId.toString).toString
        )

        def req(uri: Uri, token: String) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests",
          headers = Headers("Authorization" -> s"Bearer $token")
        )
          .withEntity(payload)

        for {
          // TODO: exception
          uri   <- app.dac.uri.fold(IO.raiseError(new Exception("DAC uri not found")))(IO(_))
          token <- app.dac.token.fold(IO.raiseError(new Exception("DAC token not found")))(IO(_))
          _     <- logger.info(s"Sending request to $uri \n ${payload.asJson}")
          res   <- c.successful(req(uri, token))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

    }

}
