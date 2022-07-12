package io.blindnet.privacy
package model.vocabulary.request

import java.time.LocalDateTime

import model.vocabulary.*
import cats.data.*
import cats.implicits.*
import io.blindnet.privacy.model.vocabulary.terms.Action

case class PrivacyRequest(
    id: String,
    appId: String,
    date: LocalDateTime,
    dataSubjectIds: List[DataSubject],
    demands: List[Demand]
)

object PrivacyRequest {
  val anonymousActions          = Action.Transparency.allSubCategories()
  val actionsRequiringSubjectId = Action.values.toList.filter(a => !anonymousActions.contains(a))

  def validate(pr: PrivacyRequest): ValidatedNel[String, PrivacyRequest] = {

    if pr.dataSubjectIds.isEmpty
      && pr.demands.exists(d => actionsRequiringSubjectId.contains(d.action))
    then "One of the demands requires subject id to be specified".invalidNel
    else pr.valid
  }

}
