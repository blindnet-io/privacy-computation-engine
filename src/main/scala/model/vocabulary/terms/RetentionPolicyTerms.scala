package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*
import doobie.util.Get

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

  def parseUnsafe(str: String): RetentionPolicyTerms =
    RetentionPolicyTerms.values.find(a => a.isTerm(str)).get

  given Decoder[RetentionPolicyTerms] =
    Decoder.decodeString.emap(RetentionPolicyTerms.parse(_).toEither)

  given Encoder[RetentionPolicyTerms] =
    Encoder[String].contramap(_.encode)

  given Get[RetentionPolicyTerms] =
    Get[String].map(t => RetentionPolicyTerms.parseUnsafe(t))

}
