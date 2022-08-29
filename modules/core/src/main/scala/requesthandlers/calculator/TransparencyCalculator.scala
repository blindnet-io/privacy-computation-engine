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
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction],
      r: Recommendation
  ): IO[PrivacyResponse] =
    for {
      rEventId     <- UUIDGen.randomUUID[IO].map(ResponseEventId.apply)
      responseTime <- Clock[IO].realTimeInstant
      newResp      <-
        r.status match {
          case Some(Status.Granted) | None =>
            for {
              answer <- getAnswer(resp.action, appId, t, ds, restr)
              // format: off
              newResp = PrivacyResponse(resp.id, rEventId, resp.demandId, responseTime, resp.action, Status.Granted, answer = Some(answer))
              // format: on
            } yield newResp

          case Some(s) =>
            // format: off
            IO.pure(PrivacyResponse(resp.id, rEventId, resp.demandId, responseTime, resp.action, s, r.motive))
            // format: on
        }
    } yield newResp

  private def getAnswer(
      a: Action,
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ): IO[Json] =
    a match {
      case Transparency          => IO(true.asJson)
      case TDataCategories       => getDataCategories(appId, t, ds, restr).json
      case TDPO                  => getDpo(appId).json
      case TKnown                => getUserKnown(appId, ds).json
      case TLegalBases           => lbRepo.get(appId, scope = false).json
      case TOrganization         => getOrganization(appId).json
      case TPolicy               => getPrivacyPolicy(appId).json
      case TProcessingCategories => getProcessingCategories(appId, t, ds, restr).json
      case TProvenance           => prRepo.get(appId).json
      case TPurpose              => getPurposes(appId, t, ds, restr).json
      case TRetention            => rpRepo.get(appId).json
      case TWhere                => getWhere(appId).json
      case TWho                  => getWho(appId).json
      case _                     => IO.raiseError(new NotImplementedError)
    }

  private def getRestrictionScope(restr: List[Restriction]) =
    Restriction
      .get[Restriction.PrivacyScope](restr)
      .toNel
      .map(_.foldLeft(PrivacyScope.empty)(_ union _.scope))

  private def getEligibilePS(appId: UUID, t: Instant, ds: DataSubject) =
    for {
      timeline <- repos.events.getTimeline(appId, ds)
      ePS = timeline.eligiblePrivacyScope(Some(t))
    } yield ePS

  private def intersect(appId: UUID, eps: PrivacyScope, rps: PrivacyScope) =
    for {
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      fPS = rps.zoomIn(selectors) intersection eps.zoomIn(selectors)
    } yield fPS

  private def getDataCategories(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ) = {
    val rPS = getRestrictionScope(restr)

    ds match {
      case None =>
        for {
          appDCs    <- psRepo.getDataCategories(appId)
          selectors <- repos.privacyScope.getSelectors(appId, active = true)
          finalDCs = rPS.fold(appDCs)(
            _.zoomIn(selectors).triples.map(_.dataCategory) intersect appDCs
          )
        } yield finalDCs

      case Some(ds) =>
        for {
          ePS    <- getEligibilePS(appId, t, ds)
          fPS    <- rPS.fold(IO(ePS))(intersect(appId, ePS, _))
          appDCs <- psRepo.getDataCategories(appId)
          finalDCs = appDCs intersect fPS.triples.map(_.dataCategory)
        } yield finalDCs
    }
  }

  private def getProcessingCategories(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ) = {
    val rPS = getRestrictionScope(restr)

    ds match {
      case None =>
        for {
          appPCs    <- psRepo.getProcessingCategories(appId)
          selectors <- repos.privacyScope.getSelectors(appId, active = true)
          finalPCs = rPS.fold(appPCs)(
            _.zoomIn(selectors).triples.map(_.processingCategory) intersect appPCs
          )
        } yield finalPCs

      case Some(ds) =>
        for {
          ePS    <- getEligibilePS(appId, t, ds)
          fPS    <- rPS.fold(IO(ePS))(intersect(appId, ePS, _))
          appPCs <- psRepo.getProcessingCategories(appId)
          finalPCs = appPCs intersect fPS.triples.map(_.processingCategory)
        } yield finalPCs
    }
  }

  private def getPurposes(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ) = {
    val rPS = getRestrictionScope(restr)

    ds match {
      case None =>
        for {
          appPPs    <- psRepo.getPurposes(appId)
          selectors <- repos.privacyScope.getSelectors(appId, active = true)
          finalPPs = rPS.fold(appPPs)(
            _.zoomIn(selectors).triples.map(_.purpose) intersect appPPs
          )
        } yield finalPPs

      case Some(ds) =>
        for {
          ePS    <- getEligibilePS(appId, t, ds)
          fPS    <- rPS.fold(IO(ePS))(intersect(appId, ePS, _))
          appPPs <- psRepo.getPurposes(appId)
          finalPPs = appPPs intersect fPS.triples.map(_.purpose)
        } yield finalPPs
    }
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
