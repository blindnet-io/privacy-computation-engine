package io.blindnet.pce
package requesthandlers

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

class TransparencyDemands(
    repositories: Repositories
) {

  import Action.*

  val giRepo = repositories.generalInfo
  val psRepo = repositories.privacyScope
  val lbRepo = repositories.legalBase
  val rpRepo = repositories.retentionPolicy
  val prRepo = repositories.provenance
  val dsRepo = repositories.dataSubject

  extension [T](io: IO[Option[T]])
    def failIfNotFound = io.orNotFound("Requested app could not be found")

  extension [T: Encoder](io: IO[T])
    def json =
      io.map(_.asJson)

  def getAnswer(
      demand: Demand,
      appId: UUID,
      ds: Option[DataSubject]
  ): IO[Json] = {
    demand.action match {
      case Transparency          => processTransparency(appId, ds).json
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
  }

  private def processTransparency(
      appId: UUID,
      ds: Option[DataSubject]
  ): IO[Map[Action, Json]] = {
    val all = List(
      psRepo.getDataCategories(appId).json.map((TDataCategories, _)),
      getDpo(appId).json.map((TDPO, _)),
      getUserKnown(appId, ds).json.map((TKnown, _)),
      lbRepo.get(appId, scope = false).json.map((TLegalBases, _)),
      getOrganization(appId).json.map((TOrganization, _)),
      getPrivacyPolicy(appId).json.map((TPolicy, _)),
      psRepo.getProcessingCategories(appId).json.map((TProcessingCategories, _)),
      prRepo.get(appId).json.map((TProvenance, _)),
      psRepo.getPurposes(appId).json.map((TPurpose, _)),
      rpRepo.get(appId).json.map((TRetention, _)),
      getWhere(appId).json.map((TWhere, _)),
      getWho(appId).json.map((TWho, _))
    ).parSequence

    all.map(_.toMap)
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
