package io.blindnet.pce
package priv
package privacyrequest

import java.util.UUID

import cats.data.*
import cats.implicits.*
import terms.*

case class Demand(
    id: UUID,
    reqId: UUID,
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
