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

class TransparencyCalculator(
    repos: Repositories
) {

  import Action.*

  val giRepo = repos.generalInfo
  val psRepo = repos.privacyScope
  val lbRepo = repos.legalBase
  val rpRepo = repos.retentionPolicy
  val prRepo = repos.provenance
  val dsRepo = repos.dataSubject

  extension [T](io: IO[Option[T]])
    def failIfNotFound = io.orNotFound("Requested app could not be found")

  extension [T: Encoder](io: IO[T])
    def json =
      io.map(_.asJson)

  def createResponse(
      resp: PrivacyResponse,
      pr: PrivacyRequest,
      r: Recommendation
  ): IO[PrivacyResponse] =
    for {
      id        <- UUIDGen.randomUUID[IO]
      timestamp <- Clock[IO].realTimeInstant
      newResp   <-
        r.status match {
          case Some(Status.Granted) | None =>
            for {
              answer <- getAnswer(resp.action, pr.appId, pr.dataSubject)
              // format: off
              newResp = PrivacyResponse(id, resp.responseId, resp.demandId, timestamp, resp.action, Status.Granted, answer = Some(answer))
              // format: on
            } yield newResp

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(id, resp.responseId, resp.demandId, timestamp, resp.action, s, r.motive))
            // format: on
        }
    } yield newResp

  private def getAnswer(a: Action, appId: UUID, ds: Option[DataSubject]): IO[Json] =
    a match {
      case Transparency          => IO(true.asJson)
      case TDataCategories       => psRepo.getDataCategories(appId).json
      case TDPO                  => getDpo(appId).json
      case TKnown                => getUserKnown(appId, ds).json
      case TLegalBases           => lbRepo.get(appId, scope = false).json
      case TOrganization         => getOrganization(appId).json
      case TPolicy               => getPrivacyPolicy(appId).json
      case TProcessingCategories => psRepo.getProcessingCategories(appId).json
      case TProvenance           => prRepo.get(appId).json
      case TPurpose              => psRepo.getPurposes(appId).json
      case TRetention            => rpRepo.get(appId).json
      case TWhere                => getWhere(appId).json
      case TWho                  => getWho(appId).json
      case _                     => IO.raiseError(new NotImplementedError)
    }

  private def getDpo(appId: UUID): IO[String] =
    giRepo
      .get(appId)
      .failIfNotFound
      .map(_.dpo)

  private def getOrganization(appId: UUID): IO[String] =
    giRepo
      .get(appId)
      .failIfNotFound
      .map(_.organization)

  private def getPrivacyPolicy(appId: UUID): IO[Option[String]] =
    giRepo
      .get(appId)
      .failIfNotFound
      .map(_.privacyPolicyLink)

  private def getUserKnown(appId: UUID, ds: Option[DataSubject]): IO[BooleanTerms] =
    ds match {
      case None     => IO(BooleanTerms.No)
      case Some(ds) =>
        dsRepo.exist(appId, ds.id).map(r => if r then BooleanTerms.Yes else BooleanTerms.No)
    }

  private def getWhere(appId: UUID): IO[List[String]] =
    giRepo
      .get(appId)
      .failIfNotFound
      .map(_.countries)

  private def getWho(appId: UUID): IO[List[String]] =
    giRepo
      .get(appId)
      .failIfNotFound
      .map(_.dataConsumerCategories)

}
