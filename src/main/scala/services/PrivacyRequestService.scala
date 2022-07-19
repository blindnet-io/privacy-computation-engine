package io.blindnet.privacy
package services

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.UUIDGen
import cats.implicits.*
import api.endpoints.payload.PrivacyRequestPayload
import db.repositories.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*
import services.requests.TransparencyDemands

class PrivacyRequestService(
    giRepo: GeneralInfoRepository,
    psRepo: PrivacyScopeRepository,
    lbRepo: LegalBaseRepository
) {

  val transparency = new TransparencyDemands(giRepo, psRepo, lbRepo)

  def getPrivacyRequest(req: PrivacyRequestPayload, appId: String) = {
    for {
      // TODO: reject if number of demands is large

      reqId     <- UUIDGen.randomUUID[IO]
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      time      <- Clock[IO].realTimeInstant

      demands = req.demands.zip(demandIds).map {
        case (d, id) =>
          Demand(
            d.id,
            id.toString(),
            d.action,
            d.message,
            d.language,
            d.data,
            // TODO: restrictions
            List.empty,
            d.target.getOrElse(Target.System)
          )
      }

      pr = PrivacyRequest(
        reqId.toString(),
        appId,
        time,
        req.dataSubject.map(ds => DataSubject(ds.id)),
        demands
      )

      _ <- PrivacyRequest
        .validate(pr)
        .fold(e => IO.raiseError(ValidationException(e)), IO.pure)

    } yield pr
  }

  def processRequest(pr: PrivacyRequest): IO[Map[String, Json]] = {
    pr.demands
      .parTraverse(d => processDemand(d, pr.appId, pr.dataSubjectIds).map(r => d.refId -> r))
      .map(_.toMap)
  }

  private def processDemand(demand: Demand, appId: String, userIds: List[DataSubject]): IO[Json] = {
    demand.action match {
      case Action.Transparency    => transparency.processTransparency(appId, userIds).map(_.asJson)
      case Action.TDataCategories => transparency.getDataCategories(appId).map(_.asJson)
      case Action.TDPO            => transparency.getDpo(appId).map(_.asJson)
      case Action.TKnown          => transparency.getUserKnown(appId, userIds).map(_.asJson)
      case Action.TLegalBases     => transparency.getLebalBases(appId, userIds).map(_.asJson)
      case Action.TOrganization   => transparency.getOrganization(appId).map(_.asJson)
      case Action.TPolicy         => transparency.getPrivacyPolicy(appId).map(_.asJson)
      case Action.TProcessingCategories =>
        transparency.getProcessingCategories(appId, userIds).map(_.asJson)
      case Action.TProvenance           => transparency.getProvenances(appId, userIds).map(_.asJson)
      case Action.TPurpose              => transparency.getPurposes(appId, userIds).map(_.asJson)
      case Action.TRetention            => transparency.getRetentions(appId, userIds).map(_.asJson)
      case Action.TWhere                => transparency.getWhere(appId).map(_.asJson)
      case Action.TWho                  => transparency.getWho(appId).map(_.asJson)
      case _                            => IO.raiseError(new NotImplementedError)
    }
  }

}
