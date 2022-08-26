package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import sttp.tapir.*
import doobie.util.Get

enum Action(term: String, parent: Option[Action] = None) {
  case Access          extends Action("ACCESS")
  case Delete          extends Action("DELETE")
  case Modify          extends Action("MODIFY")
  case Object          extends Action("OBJECT")
  case Portability     extends Action("PORTABILITY")
  case Restrict        extends Action("RESTRICT")
  case RevokeConsent   extends Action("REVOKE-CONSENT")
  case Transparency    extends Action("TRANSPARENCY")
  case TDataCategories extends Action("TRANSPARENCY.DATA-CATEGORIES", Some(Transparency))
  case TDPO            extends Action("TRANSPARENCY.DPO", Some(Transparency))
  case TKnown          extends Action("TRANSPARENCY.KNOWN", Some(Transparency))
  case TLegalBases     extends Action("TRANSPARENCY.LEGAL-BASES", Some(Transparency))
  case TOrganization   extends Action("TRANSPARENCY.ORGANIZATION", Some(Transparency))
  case TPolicy         extends Action("TRANSPARENCY.POLICY", Some(Transparency))
  case TProcessingCategories
      extends Action("TRANSPARENCY.PROCESSING-CATEGORIES", Some(Transparency))

  case TProvenance extends Action("TRANSPARENCY.PROVENANCE", Some(Transparency))
  case TPurpose    extends Action("TRANSPARENCY.PURPOSE", Some(Transparency))
  case TRetention  extends Action("TRANSPARENCY.RETENTION", Some(Transparency))
  case TWhere      extends Action("TRANSPARENCY.WHERE", Some(Transparency))
  case TWho        extends Action("TRANSPARENCY.WHO", Some(Transparency))
  case Other       extends Action("OTHER")

  def withSubCategories(): List[Action] = {
    val children = Action.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.withSubCategories())
  }

  def getMostGranularSubcategories(): List[Action] = {
    val children = Action.values.filter(_.isChildOf(this)).toList
    children.flatMap(_.withSubCategories())
  }

  def isChildOf(a: Action) = parent.exists(_ == a)

  def isTerm(str: String) = term == str

  val encode = term
}

object Action {
  def parse(str: String): Validated[String, Action] =
    Validated.fromOption(
      Action.values.find(a => a.isTerm(str)),
      "Unknown action"
    )

  def parseUnsafe(str: String): Action =
    Action.values.find(a => a.isTerm(str)).get

  given Decoder[Action] =
    Decoder.decodeString.emap(Action.parse(_).toEither)

  given Encoder[Action] =
    Encoder[String].contramap(_.encode)

  given KeyEncoder[Action] =
    KeyEncoder[String].contramap(_.encode)

  given Schema[Action] =
    Schema.string.validate(Validator.enumeration(Action.values.toList, x => Option(x.encode)))

  given Get[Action] =
    Get[String].tmap(t => Action.parseUnsafe(t))

}
