package io.blindnet.privacy
package model.vocabulary.request

import cats.data.*
import cats.implicits.*
import model.vocabulary.terms.Action
import model.vocabulary.DataSubject
import model.vocabulary.terms.Target

case class Demand(
    id: String,
    reqId: String,
    action: Action,
    message: Option[String],
    // TODO: parse https://datatracker.ietf.org/doc/rfc5646/
    language: Option[String],
    // TODO: what format?
    data: List[String],
    restrictions: List[Restriction]
)

object Demand {
  def validate(demand: Demand): ValidatedNel[String, Demand] = {
    // TODO: validate restrictions
    demand.valid
  }

}
