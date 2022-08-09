package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import sttp.tapir.*
import doobie.util.Get

enum Target(term: String, parent: Option[Target] = None) {
  case All          extends Target("*")
  case Organization extends Target("ORGANIZATION", Some(All))
  case System       extends Target("SYSTEM", Some(All))
  case Partners     extends Target("PARTNERS", Some(All))
  case PDownward    extends Target("PARTNERS.DOWNWARD", Some(Partners))
  case PUpward      extends Target("PARTNERS.UPWARD", Some(Partners))

  def allSubCategories(): List[Target] = {
    val children = Target.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  def isChildOf(a: Target) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str

  val encode = term
}

object Target {
  def parse(str: String): Validated[String, Target] =
    Validated.fromOption(
      Target.values.find(a => a.isTerm(str)),
      "Unknown target"
    )

  def parseUnsafe(str: String): Target =
    Target.values.find(a => a.isTerm(str)).get

  given Decoder[Target] =
    Decoder.decodeString.emap(Target.parse(_).toEither)

  given Encoder[Target] =
    Encoder[String].contramap(_.encode)

  given Schema[Target] =
    Schema.string.validate(Validator.enumeration(Target.values.toList, x => Option(x.encode)))

  given Get[Target] =
    Get[String].tmap(t => Target.parseUnsafe(t))

}
