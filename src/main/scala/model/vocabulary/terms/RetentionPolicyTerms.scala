package io.blindnet.privacy
package model.vocabulary.terms

enum RetentionPolicyTerms {
  case NoLongerThan
  case NoLessThan
}

object RetentionPolicyTerms {
  def parse(s: String): Option[RetentionPolicyTerms] =
    s match {
      case "NO-LONGER-THAN" => Some(RetentionPolicyTerms.NoLongerThan)
      case "NO-LESS-THAN"   => Some(RetentionPolicyTerms.NoLessThan)
      case _                => None
    }

}
