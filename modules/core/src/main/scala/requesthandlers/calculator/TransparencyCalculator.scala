package io.blindnet.pce
package requesthandlers.calculator

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.std.UUIDGen
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
              strAnswer = answer.spaces2
              // format: off
              newResp = PrivacyResponse(resp.id, rEventId, resp.demandId, responseTime, resp.action, Status.Granted, answer = Some(strAnswer))
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
      case TLegalBases           => getLegalBases(appId, t, ds).json // TODO: restrictions
      case TOrganization         => getOrganization(appId).json
      case TPolicy               => getPrivacyPolicy(appId).json
      case TProcessingCategories => getProcessingCategories(appId, t, ds, restr).json
      case TProvenance           => getProvenances(appId, t, ds, restr).json
      case TPurpose              => getPurposes(appId, t, ds, restr).json
      case TRetention            => getRetentionPolicies(appId, t, ds, restr).json
      case TWhere                => getWhere(appId).json
      case TWho                  => getWho(appId).json
      case _                     => IO.raiseError(new NotImplementedError)
    }

  private def getRestrictionScope(restr: List[Restriction]) =
    Restriction
      .cast[Restriction.PrivacyScope](restr)
      .toNel
      .map(_.foldLeft(PrivacyScope.empty)(_ union _.scope))

  private def getEligibilePS(appId: UUID, t: Instant, ds: DataSubject) =
    for {
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      ctx = PSContext(selectors)
      timeline    <- repos.events.getTimeline(ds, ctx)
      regulations <- repos.regulations.get(appId, ctx)
      ePS = timeline.eligiblePrivacyScope(Some(t), regulations)
    } yield ePS

  private def intersect(appId: UUID, eps: PrivacyScope, rps: PrivacyScope) =
    for {
      selectors <- repos.privacyScope.getSelectors(appId, active = true)
      ctx = PSContext(selectors)
      fPS = rps.zoomIn(ctx) intersection eps
    } yield fPS

  // TODO: there is a lot of repeating code in the following methods. refactor!
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
          ctx      = PSContext(selectors)
          finalDCs = rPS.fold(appDCs)(_.zoomIn(ctx).triples.map(_.dataCategory) intersect appDCs)
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
          ctx      = PSContext(selectors)
          finalPCs = rPS.fold(appPCs)(
            _.zoomIn(ctx).triples.map(_.processingCategory) intersect appPCs
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
          ctx      = PSContext(selectors)
          finalPPs = rPS.fold(appPPs)(
            _.zoomIn(ctx).triples.map(_.purpose) intersect appPPs
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

  private def getProvenances(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ) = {
    val rPS = getRestrictionScope(restr)

    ds match {
      case None =>
        for {
          provenances <- prRepo.get(appId)
          selectors   <- repos.privacyScope.getSelectors(appId, active = true)
          ctx          = PSContext(selectors)
          fProvenances = rPS.fold(provenances)(
            rps =>
              val dcs = rps.zoomIn(ctx).triples.map(_.dataCategory)
              provenances.filter(p => dcs.contains(p._1))
          )
        } yield fProvenances

      case Some(ds) =>
        for {
          ePS <- getEligibilePS(appId, t, ds)
          fPS <- rPS.fold(IO(ePS))(intersect(appId, ePS, _))
          dcs = fPS.triples.map(_.dataCategory)
          provenances <- prRepo.get(appId)
          filtered = provenances.filter(p => dcs.contains(p._1))
        } yield filtered
    }
  }

  private def getRetentionPolicies(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject],
      restr: List[Restriction]
  ) = {
    val rPS = getRestrictionScope(restr)

    ds match {
      case None =>
        for {
          retentionPolicies <- rpRepo.get(appId)
          selectors         <- repos.privacyScope.getSelectors(appId, active = true)
          ctx         = PSContext(selectors)
          filteredRPs = rPS.fold(retentionPolicies)(
            rps =>
              val dcs = rps.zoomIn(ctx).triples.map(_.dataCategory)
              retentionPolicies.filter(p => dcs.contains(p._1))
          )
        } yield filteredRPs

      case Some(ds) =>
        for {
          ePS <- getEligibilePS(appId, t, ds)
          fPS <- rPS.fold(IO(ePS))(intersect(appId, ePS, _))
          dcs = fPS.triples.map(_.dataCategory)
          retentionPolicies <- rpRepo.get(appId)
          filtered = retentionPolicies.filter(p => dcs.contains(p._1))
        } yield filtered
    }
  }

  private def getLegalBases(
      appId: UUID,
      t: Instant,
      ds: Option[DataSubject]
  ) = {
    ds match {
      case None     => lbRepo.get(appId, scope = false)
      case Some(ds) =>
        lbRepo.get(appId, scope = false).json
        for {
          timeline <- repos.events.getTimeline(ds, PSContext.empty)
          lbIds = timeline.compiledEvents(Some(t)).flatMap(_.getLbId)
          lbs <- lbRepo.get(appId, scope = false)
          res = lbs.filter(lb => lbIds.contains(lb.id))
        } yield res
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
