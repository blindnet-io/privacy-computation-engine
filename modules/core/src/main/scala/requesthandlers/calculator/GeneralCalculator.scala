package io.blindnet.pce
package requesthandlers.calculator

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.implicits.*
import io.blindnet.pce.util.extension.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{ Encoder, Json }
import db.repositories.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*
import model.error.*
import cats.effect.std.UUIDGen
import io.blindnet.pce.services.external.StorageInterface
import io.blindnet.pce.model.*

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
      newRespId <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant
      newResp   <-
        r.status match {
          case Some(Status.Granted) =>
            createGrantedResponse(resp.responseId, pr.appId, d, ccr, pr.dataSubject.get, r)

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, s, r.motive))
            // format: on

          case None =>
            // format: off
            IO.pure(PrivacyResponse(newRespId, resp.responseId, d.id, timestamp, d.action, Status.Denied, r.motive))
            // format: on
        }
    } yield newResp

  def createGrantedResponse(
      respId: UUID,
      appId: UUID,
      d: Demand,
      ccr: CommandCreateResponse,
      ds: DataSubject,
      r: Recommendation
  ) =
    for {
      newRespId <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant

      msg  = ccr.data.hcursor.downField("msg").as[String].toOption
      lang = ccr.data.hcursor.downField("lang").as[String].toOption

      newResp = PrivacyResponse(
        newRespId,
        respId,
        d.id,
        timestamp,
        d.action,
        Status.Granted,
        message = msg,
        lang = lang
      )
    } yield newResp

}
