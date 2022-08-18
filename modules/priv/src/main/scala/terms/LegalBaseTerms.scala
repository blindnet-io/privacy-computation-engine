package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import doobie.util.Get
import sttp.tapir.Schema
import sttp.tapir.Validator

enum LegalBaseTerms(term: String, parent: Option[LegalBaseTerms] = None) {
  case Contract           extends LegalBaseTerms("CONTRACT")
  case Consent            extends LegalBaseTerms("CONSENT")
  case LegitimateInterest extends LegalBaseTerms("LEGITIMATE-INTEREST")
  case Necessary          extends LegalBaseTerms("NECESSARY")
  case NLegalObligation   extends LegalBaseTerms("NECESSARY.LEGAL-OBLIGATION", Some(Necessary))
  case NPublicInterest    extends LegalBaseTerms("NECESSARY.PUBLIC-INTEREST", Some(Necessary))
  case NVitalInterest     extends LegalBaseTerms("NECESSARY.VITAL-INTEREST", Some(Necessary))
  case Other              extends LegalBaseTerms("OTHER-LEGAL-BASE")

  def allSubCategories(): List[LegalBaseTerms] = {
    val children = LegalBaseTerms.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  private def isChildOf(a: LegalBaseTerms) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str

  val encode = term
}

object LegalBaseTerms {
  def parse(str: String): Validated[String, LegalBaseTerms] =
    Validated.fromOption(
      LegalBaseTerms.values.find(a => a.isTerm(str)),
      "Unknown legal base"
    )

  def parseUnsafe(str: String): LegalBaseTerms =
    LegalBaseTerms.values.find(a => a.isTerm(str)).get

  given Decoder[LegalBaseTerms] =
    Decoder.decodeString.emap(LegalBaseTerms.parse(_).toEither)

  given Encoder[LegalBaseTerms] =
    Encoder[String].contramap(_.encode)

  given Schema[LegalBaseTerms] =
    Schema.string.validate(
      Validator.enumeration(LegalBaseTerms.values.toList, x => Option(x.encode))
    )

  given Get[LegalBaseTerms] =
    Get[String].tmap(t => LegalBaseTerms.parseUnsafe(t))

}
