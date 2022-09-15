package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import sttp.tapir.{Schema, Validator}

case class ProcessingCategory(term: String)

object ProcessingCategory {

  def parse(s: String): Validated[String, ProcessingCategory] =
    Validated.fromOption(
      terms.find(_ == s).map(_ => ProcessingCategory(s)),
      "Unknown processing category"
    )

  def granularize(dc: ProcessingCategory): Set[ProcessingCategory] = {
    def granularize(term: String): List[String] =
      val n = terms.filter(t => t.startsWith(s"$term."))
      if n.length == 0 then List(term) else n.flatMap(t => granularize(t))

    val res =
      if dc.term == "*" then terms.tail.flatMap(t => granularize(t))
      else granularize(dc.term)

    res.toSet.map(ProcessingCategory(_))
  }

  val All = ProcessingCategory("*")

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

  given Schema[ProcessingCategory] =
    Schema.string.validate(
      Validator.enumeration(terms.map(ProcessingCategory(_)), x => Option(x.term))
    )

}
