package io.blindnet.pce
package clients

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
import io.blindnet.pce.clients.*

trait StorageClient {
  def get(
      app: PCEApp,
      callbackId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

  def delete(
      app: PCEApp,
      callbackId: UUID,
      subject: DataSubject,
      rec: Recommendation
  ): IO[Unit]

  def privacyScopeChange(
      app: PCEApp,
      callbackId: UUID,
      subject: DataSubject,
      lost: List[(UUID, PrivacyScope)]
  ): IO[Unit]

}

object StorageClient {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def live(c: Client[IO], repos: Repositories, conf: Config) =
    new StorageClient {
      def get(
          app: PCEApp,
          callbackId: UUID,
          subject: DataSubject,
          rec: Recommendation
      ): IO[Unit] = {

        val payload = DataRequestPayload(
          DataQueryPayload(
            selectors = rec.dataCategories.map(_.term).toList,
            subjects = List(subject.id),
            provenance = rec.provenance.map(_.encode),
            target = rec.target.map(_.encode),
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          callback = conf.callbackUri / "callback" / callbackId.toString
        )

        def req(uri: Uri, token: String) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests" / "get",
          headers = Headers("Authorization" -> s"Bearer $token")
        )
          .withEntity(payload)

        for {
          // TODO: exception
          uri   <- app.dac.uri.fold(IO.raiseError(new Exception("DAC uri not found")))(IO(_))
          token <- app.dac.token.fold(IO.raiseError(new Exception("DAC token not found")))(IO(_))
          _     <- logger.info(s"Sending get request to $uri \n ${payload.asJson}")
          res   <- c.successful(req(uri, token))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

      def delete(
          app: PCEApp,
          callbackId: UUID,
          subject: DataSubject,
          rec: Recommendation
      ): IO[Unit] = {
        val payload = DataRequestPayload(
          DataQueryPayload(
            selectors = rec.dataCategories.map(_.term).toList,
            subjects = List(subject.id),
            provenance = rec.provenance.map(_.encode),
            target = rec.target.map(_.encode),
            after = rec.dateFrom,
            until = rec.dateTo
          ),
          callback = conf.callbackUri / "callback" / callbackId.toString
        )

        def req(uri: Uri, token: String) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests" / "delete",
          headers = Headers("Authorization" -> s"Bearer $token")
        )
          .withEntity(payload)

        for {
          // TODO: exception
          uri   <- app.dac.uri.fold(IO.raiseError(new Exception("DAC uri not found")))(IO(_))
          token <- app.dac.token.fold(IO.raiseError(new Exception("DAC token not found")))(IO(_))
          _     <- logger.info(s"Sending delete request to $uri \n ${payload.asJson}")
          res   <- c.successful(req(uri, token))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

      def privacyScopeChange(
          app: PCEApp,
          callbackId: UUID,
          subject: DataSubject,
          lost: List[(UUID, PrivacyScope)]
      ): IO[Unit] = {
        val query = lost.map(l => PrivacyScopeItem(l._1, l._2))

        val payload =
          PrivacyScopePayload(
            subjects = List(subject.id),
            lost = query,
            callback = conf.callbackUri / "callback" / callbackId.toString
          )

        def req(uri: Uri, token: String) = Request[IO](
          method = Method.POST,
          uri = uri / "v1" / "requests" / "privacy_scope",
          headers = Headers("Authorization" -> s"Bearer $token")
        )
          .withEntity(payload)

        for {
          // TODO: exception
          uri   <- app.dac.uri.fold(IO.raiseError(new Exception("DAC uri not found")))(IO(_))
          token <- app.dac.token.fold(IO.raiseError(new Exception("DAC token not found")))(IO(_))
          _     <- logger.info(s"Sending privacy scope request to $uri")
          res   <- c.successful(req(uri, token))
          _ = if res then IO.unit else IO.raiseError(InternalException("Non 200 response from DAC"))
        } yield ()
      }

    }

}
