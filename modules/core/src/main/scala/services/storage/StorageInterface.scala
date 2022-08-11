package io.blindnet.pce
package services.storage

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

trait StorageInterface {
  def requestAccessLink(
      id: UUID,
      appId: UUID,
      dId: UUID,
      subject: List[DataSubject],
      rec: Recommendation
  ): IO[Unit]

}

object StorageInterface {
  def live(c: Client[IO], conf: Config) =
    new StorageInterface {
      def requestAccessLink(
          id: UUID,
          appId: UUID,
          dId: UUID,
          subject: List[DataSubject],
          rec: Recommendation
      ): IO[Unit] = {

        val payload = RequestAccessPayload(
          selectors = rec.dataCategories.map(_.term).toList,
          subjects = subject.map(_.id),
          provenance = rec.provenance.map(_.encode),
          target = None,
          after = rec.dateFrom,
          until = rec.dateTo,
          appId = appId.toString(),
          requestId = dId.toString(),
          callback = (conf.callbackUri / "callback" / id).toString
        )

        val req = Request[IO](
          method = Method.POST,
          uri = conf.components.dac.uri
        )
          .withEntity(payload)

        c.successful(req).flatMap {
          case true  => IO.unit
          case false => IO.raiseError(InternalException("Non 200 response from DCA"))
        }
      }

    }

}
