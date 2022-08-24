package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID

import cats.data.*
import cats.implicits.*
import terms.*

case class PrivacyRequest(
    id: UUID,
    appId: UUID,
    timestamp: Instant,
    target: Target,
    email: Option[String],
    dataSubject: Option[DataSubject],
    providedDsIds: List[String],
    demands: List[Demand]
)

object PrivacyRequest {
  def validateDemands(pr: PrivacyRequest): (List[(NonEmptyList[String], Demand)], List[Demand]) = {
    pr.demands.foldLeft((List.empty[(NonEmptyList[String], Demand)], List.empty[Demand]))(
      (acc, cur) => {
        Demand
          .validate(cur)
          .fold(
            errs => ((errs, cur) :: acc._1, acc._2),
            d => (acc._1, cur :: acc._2)
          )
      }
    )
  }

}
