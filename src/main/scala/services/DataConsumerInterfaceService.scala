package io.blindnet.privacy
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.privacy.api.endpoints.messages.consumerinterface.*
import io.blindnet.privacy.model.error.given
import io.blindnet.privacy.services.util.*
import io.blindnet.privacy.util.extension.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*

class DataConsumerInterfaceService(
    repos: Repositories
) {

  def getPendingDemands(appId: UUID) = {
    for {
      dIds    <- repos.pendingDemands.getPendingDemandIds(appId)
      demands <- NonEmptyList.fromList(dIds) match {
        case None      => IO(List.empty[Demand])
        case Some(ids) => repos.privacyRequest.getDemands(ids)
      }
      reqs    <- NonEmptyList.fromList(demands.map(_.reqId)) match {
        case None      => IO(List.empty[PrivacyRequest])
        case Some(ids) => repos.privacyRequest.getRequestsSimple(ids)
      }

      res = (demands.sortBy(_.reqId) zip reqs.sortBy(_.id)).map(PendingDemandPayload.fromPrivDemand)
    } yield res
  }

  def getPendingDemandDetails(appId: UUID, dId: UUID) =
    for {
      _ <- repos.privacyRequest.demandExist(appId, dId).emptyNotFound(s"Demand $dId not found")
      d <- repos.privacyRequest.getDemand(dId).orNotFound(s"Demand $dId not found")
      rId = d.reqId
      req <- repos.privacyRequest.getRequestSimple(rId).orNotFound(s"Request $rId not found")
      rec <- repos.privacyRequest.getRecommendation(dId)
      res = PendingDemandDetailsPayload.fromPrivDemand(d, req, rec)
    } yield res

}
