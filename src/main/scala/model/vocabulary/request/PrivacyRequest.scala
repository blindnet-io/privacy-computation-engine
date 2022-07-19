package io.blindnet.privacy
package model.vocabulary.request

import java.time.Instant

import cats.data.*
import cats.implicits.*
import cats.kernel.Semigroup
import model.vocabulary.*
import model.vocabulary.terms.Action

case class PrivacyRequest(
    id: String,
    appId: String,
    timestamp: Instant,
    dataSubjectIds: List[DataSubject],
    demands: List[Demand]
)

object PrivacyRequest {
  val anonymousActions          = Action.Transparency.allSubCategories()
  val actionsRequiringSubjectId = Action.values.toList.filter(a => !anonymousActions.contains(a))

  def validate(pr: PrivacyRequest): ValidatedNel[String, Unit] = {

    def validateDataSubjects =
      if pr.dataSubjectIds.isEmpty
        && pr.demands.exists(d => actionsRequiringSubjectId.contains(d.action))
      then "One of the demands requires subject id to be specified".invalidNel
      else ().valid

    def validateDemands =
      pr.demands.traverse_(Demand.validate)

    validateDataSubjects combine validateDemands
  }

}
