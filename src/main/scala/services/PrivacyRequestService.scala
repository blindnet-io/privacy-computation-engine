package io.blindnet.privacy
package services

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.payload.*
import db.repositories.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*
import services.requests.TransparencyDemands

class PrivacyRequestService(
    repositories: Repositories
) {

  val transparency = new TransparencyDemands(repositories)

  private def createPrivacyRequest(req: PrivacyRequestPayload, appId: String) = {
    for {
      // TODO: reject if number of demands is large

      reqId     <- UUIDGen.randomUUID[IO]
      demandIds <- UUIDGen.randomUUID[IO].replicateA(req.demands.length)
      time      <- Clock[IO].realTimeInstant

      _ <-
        if req.demands.map(_.id).toSet.size == req.demands.size then IO.unit
        else IO.raiseError(BadRequestException("Demands have duplicate ids"))

      demands = req.demands.zip(demandIds).map {
        case (d, id) =>
          Demand(
            d.id,
            id.toString(),
            d.action,
            d.message,
            d.language,
            d.data.getOrElse(List.empty),
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
        .fold(e => IO.raiseError(BadRequestException(e.toList.mkString("\n"))), IO.pure)

    } yield pr
  }

  def processRequest(
      req: PrivacyRequestPayload,
      appId: String
  ): IO[PrivacyRequestResponsePayload] = {
    for {
      pr      <- createPrivacyRequest(req, appId)
      // TODO: store request
      results <- pr.demands
        .parTraverse(
          d =>
            processDemand(d, pr.appId, pr.dataSubjectIds).map(
              r =>
                (
                  Json.obj(
                    "demand_id" -> d.refId.asJson,
                    "action"    -> d.action.asJson,
                    "result"    -> r
                  )
                )
            )
        )

    } yield PrivacyRequestResponsePayload(pr.id, results)
  }

  private def processDemand(demand: Demand, appId: String, userIds: List[DataSubject]): IO[Json] = {
    demand.action match {
      case Action.Transparency    => transparency.processTransparency(appId, userIds).map(_.asJson)
      case Action.TDataCategories => transparency.getDataCategories(appId).map(_.map(_.term).asJson)
      case Action.TDPO            => transparency.getDpo(appId).map(_.asJson)
      case Action.TKnown          => transparency.getUserKnown(appId, userIds).map(_.asJson)
      // TODO user
      case Action.TLegalBases     => transparency.getLegalBases(appId, userIds).map(_.asJson)
      case Action.TOrganization   => transparency.getOrganization(appId).map(_.asJson)
      case Action.TPolicy         => transparency.getPrivacyPolicy(appId).map(_.asJson)
      case Action.TProcessingCategories =>
        transparency.getProcessingCategories(appId, userIds).map(_.map(_.term).asJson) // TODO user
      case Action.TProvenance           =>
        transparency.getProvenances(appId, userIds).map(_.asJson) // TODO user
      case Action.TPurpose              =>
        transparency.getPurposes(appId, userIds).map(_.map(_.term).asJson) // TODO user
      case Action.TRetention            =>
        transparency
          .getRetentions(appId, userIds)
          .map(
            _.map {
              case (s, rp) =>
                Json
                  .obj(
                    "selector_id"        -> s.id.asJson,
                    "selector_name"      -> s.name.asJson,
                    "retention_policies" -> rp.asJson
                  )
            }.asJson // TODO user
          )
      case Action.TWhere                => transparency.getWhere(appId).map(_.asJson)
      case Action.TWho                  => transparency.getWho(appId).map(_.asJson)
      case _                            => IO.raiseError(new NotImplementedError)
    }
  }

}
