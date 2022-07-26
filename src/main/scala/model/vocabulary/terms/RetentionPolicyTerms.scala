package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

enum RetentionPolicyTerms(term: String) {
  case NoLongerThan extends RetentionPolicyTerms("NO-LONGER-THAN")
  case NoLessThan   extends RetentionPolicyTerms("NO-LESS-THAN")

  def isTerm(str: String) = term == str

  val encode = term
}

object RetentionPolicyTerms {
  def parse(str: String): Validated[String, RetentionPolicyTerms] =
    Validated.fromOption(
      RetentionPolicyTerms.values.find(a => a.isTerm(str)),
      "Unknown retention policy"
    )

  given Decoder[RetentionPolicyTerms] =
    Decoder.decodeString.emap(RetentionPolicyTerms.parse(_).toEither)

  given Encoder[RetentionPolicyTerms] =
    Encoder[String].contramap(_.encode)

}
