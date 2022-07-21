package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

case class ProcessingCategory(term: String)

object ProcessingCategory {

  def parse(s: String): Validated[String, ProcessingCategory] =
    Validated.fromOption(
      s.split('.').headOption.filter(terms.contains).map(_ => ProcessingCategory(s)),
      "Unknown processing category"
    )

  val terms = List(
    "*",
    "ANONYMIZATION",
    "AUTOMATED-INFERENCE",
    "AUTOMATED-DECISION-MAKING",
    "COLLECTION",
    "GENERATING",
    "PUBLISHING",
    "STORING",
    "SHARING",
    "USING",
    "OTHER-PROCESSING"
  )

  given Decoder[ProcessingCategory] =
    Decoder.decodeString.emap(ProcessingCategory.parse(_).toEither)

  given Encoder[ProcessingCategory] =
    Encoder[String].contramap(_.term)

}
