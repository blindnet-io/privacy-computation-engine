package io.blindnet.privacy
package model.vocabulary.request

import java.time.Instant

import cats.data.*
import cats.implicits.*
import cats.kernel.Semigroup
import model.vocabulary.*
import model.vocabulary.terms.*

case class PrivacyRequest(
    id: String,
    appId: String,
    timestamp: Instant,
    target: Target,
    email: Option[String],
    dataSubject: List[DataSubject],
    demands: List[Demand]
)

object PrivacyRequest {
  val anonymousActions          = Action.Transparency.allSubCategories()
  val actionsRequiringSubjectId = Action.values.toList.filter(a => !anonymousActions.contains(a))

  def validateDemands(pr: PrivacyRequest): (List[(NonEmptyList[String], Demand)], List[Demand]) = {
    pr.demands.foldLeft((List.empty[(NonEmptyList[String], Demand)], List.empty[Demand]))(
      (acc, cur) => {
        val needsDs =
          if pr.dataSubject.isEmpty && actionsRequiringSubjectId.contains(cur.action)
          then "Data subject not specified".invalid
          else cur.valid

        (Demand.validate(cur) product needsDs.toValidatedNel).fold(
          errs => ((errs, cur) :: acc._1, acc._2),
          d => (acc._1, cur :: acc._2)
        )
      }
    )
  }

}
