package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

import cats.data.Validated
import io.circe.*

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

  given Decoder[ProvenanceTerms] =
    Decoder.decodeString.emap(ProvenanceTerms.parse(_).toEither)

  given Encoder[ProvenanceTerms] =
    Encoder[String].contramap(_.encode)

}
