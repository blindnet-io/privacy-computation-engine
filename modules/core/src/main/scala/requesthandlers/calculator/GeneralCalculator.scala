package io.blindnet.pce
package requesthandlers.calculator

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.blindnet.pce.model.*
import io.blindnet.pce.services.external.StorageInterface
import io.blindnet.pce.util.extension.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{ Encoder, Json }
import db.repositories.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*
import model.error.*

class GeneralCalculator(
    repos: Repositories,
    storage: StorageInterface
) {

  import Action.*

  def createResponse(
      pr: PrivacyRequest,
      ccr: CommandCreateResponse,
      d: Demand,
      resp: PrivacyResponse,
      r: Recommendation
  ): IO[PrivacyResponse] =
    for {
      rEventId  <- UUIDGen.randomUUID[IO].map(ResponseEventId.apply)
      timestamp <- Clock[IO].realTimeInstant
      msg  = ccr.data.hcursor.downField("msg").as[String].toOption
      lang = ccr.data.hcursor.downField("lang").as[String].toOption
      newResp <-
        r.status match {
          case Some(Status.Granted) =>
            // format: off
            createGrantedResponse(resp.id, pr.appId, d, ccr, pr.dataSubject.get, r, msg, lang)
            // format: on

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(resp.id, rEventId, d.id, timestamp, d.action, s, r.motive, message = msg, lang = lang))
            // format: on

          case None =>
            // format: off
            IO.pure(PrivacyResponse(resp.id, rEventId, d.id, timestamp, d.action, Status.Denied, r.motive, message = msg, lang = lang))
            // format: on
        }
    } yield newResp

  def createGrantedResponse(
      respId: ResponseId,
      appId: UUID,
      d: Demand,
      ccr: CommandCreateResponse,
      ds: DataSubject,
      r: Recommendation,
      msg: Option[String],
      lang: Option[String]
  ) =
    for {
      rEventId  <- UUIDGen.randomUUID[IO].map(ResponseEventId.apply)
      timestamp <- Clock[IO].realTimeInstant

      newResp = PrivacyResponse(
        respId,
        rEventId,
        d.id,
        timestamp,
        d.action,
        Status.Granted,
        message = msg,
        lang = lang
      )
    } yield newResp

}
