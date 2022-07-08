package io.blindnet.privacy
package model.vocabulary.terms

enum LegalBase(term: String, parent: Option[LegalBase] = None) {
  case Contract           extends LegalBase("CONTRACT")
  case Consent            extends LegalBase("CONSENT")
  case LegitimateInterest extends LegalBase("LEGITIMATE-INTEREST")
  case Necessary          extends LegalBase("NECESSARY")
  case NLegalObligation   extends LegalBase("NECESSARY.LEGAL-OBLIGATION", Some(Necessary))
  case NPublicInterest    extends LegalBase("NECESSARY.PUBLIC-INTEREST", Some(Necessary))
  case NVitalInterest     extends LegalBase("NECESSARY.VITAL-INTEREST", Some(Necessary))
  case Other              extends LegalBase("OTHER-LEGAL-BASE")

  def allSubCategories(): List[LegalBase] = {
    val children = LegalBase.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  def isChildOf(a: LegalBase) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str
}

object LegalBase {
  def parse(str: String): Option[LegalBase] =
    LegalBase.values.find(a => a.isTerm(str))

}
