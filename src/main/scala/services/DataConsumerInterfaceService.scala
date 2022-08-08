package io.blindnet.privacy
package services

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import model.vocabulary.DataSubject
import model.vocabulary.request.{ Demand, PrivacyRequest, * }
import model.vocabulary.terms.*
import io.blindnet.privacy.model.error.given
import java.time.Instant
import io.blindnet.privacy.util.extension.*
import io.blindnet.privacy.api.endpoints.messages.consumerinterface.PendingDemandPayload

class DataConsumerInterfaceService(
    repos: Repositories
) {

  extension (s: String) {
    def failBadRequest = BadRequestException(BadPrivacyRequestPayload(s).asJson).raise
    def failNotFound   = NotFoundException(s).raise
  }

  def getPendingDemands(appId: String) = {
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

}
