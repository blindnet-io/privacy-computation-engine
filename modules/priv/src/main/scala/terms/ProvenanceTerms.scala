package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import doobie.util.Get
import io.circe.*
import sttp.tapir.{ Schema, Validator }

enum ProvenanceTerms(term: String, parent: Option[ProvenanceTerms] = None) {
  case All         extends ProvenanceTerms("*")
  case Derived     extends ProvenanceTerms("DERIVED", Some(All))
  case Transferred extends ProvenanceTerms("TRANSFERRED", Some(All))
  case User        extends ProvenanceTerms("USER", Some(All))
  case DataSubject extends ProvenanceTerms("USER.DATA-SUBJECT", Some(User))

  def allSubCategories(): List[ProvenanceTerms] = {
    val children = ProvenanceTerms.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  private def isChildOf(a: ProvenanceTerms) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str

  val encode = term
}

object ProvenanceTerms {
  def parse(str: String): Validated[String, ProvenanceTerms] =
    Validated.fromOption(
      ProvenanceTerms.values.find(a => a.isTerm(str)),
      "Unknown provenance"
    )

  def parseUnsafe(str: String): ProvenanceTerms =
    ProvenanceTerms.values.find(a => a.isTerm(str)).get

  given Decoder[ProvenanceTerms] =
    Decoder.decodeString.emap(ProvenanceTerms.parse(_).toEither)

  given Encoder[ProvenanceTerms] =
    Encoder[String].contramap(_.encode)

  given Get[ProvenanceTerms] =
    Get[String].map(t => ProvenanceTerms.parseUnsafe(t))

  given Schema[ProvenanceTerms] =
    Schema.string.validate(
      Validator.enumeration(ProvenanceTerms.values.toList, x => Option(x.encode))
    )

}
