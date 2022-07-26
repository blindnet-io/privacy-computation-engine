package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

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

  given Decoder[LegalBaseTerms] =
    Decoder.decodeString.emap(LegalBaseTerms.parse(_).toEither)

  given Encoder[LegalBaseTerms] =
    Encoder[String].contramap(_.encode)

}
