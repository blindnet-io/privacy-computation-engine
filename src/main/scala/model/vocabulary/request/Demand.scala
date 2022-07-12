package io.blindnet.privacy
package model.vocabulary.request

import model.vocabulary.terms.Action
import model.vocabulary.DataSubject
import java.lang.annotation.Target
import cats.data.*
import cats.implicits.*

case class Demand(
    id: String,
    action: Action,
    message: Option[String],
    // TODO: parse https://datatracker.ietf.org/doc/rfc5646/
    language: Option[String],
    // TODO: what format?
    data: List[String],
    restrictions: List[Restriction],
    target: Target
)

object Demand {
  def validate(demand: Demand): ValidatedNel[String, Demand] = {
    // TODO: validate restrictions
    demand.valid
  }

}
