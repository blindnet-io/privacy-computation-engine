package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

enum Target(term: String, parent: Option[Target] = None) {
  case Organization extends Target("ORGANIZATION")
  case System       extends Target("SYSTEM")
  case Partners     extends Target("PARTNERS")
  case PDownward    extends Target("PARTNERS.DOWNWARD", Some(Partners))
  case PUpward      extends Target("PARTNERS.UPWARD", Some(Partners))

  def allSubCategories(): List[Target] = {
    val children = Target.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  def isChildOf(a: Target) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str
}

object Target {
  def parse(str: String): Validated[String, Target] =
    Validated.fromOption(
      Target.values.find(a => a.isTerm(str)),
      "Unknown target"
    )

  given Decoder[Target] =
    Decoder.decodeString.emap(Target.parse(_).toEither)

}
