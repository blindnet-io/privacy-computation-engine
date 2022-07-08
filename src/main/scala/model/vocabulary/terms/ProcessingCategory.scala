package io.blindnet.privacy
package model.vocabulary.terms

case class ProcessingCategory(term: String)

object ProcessingCategory {

  def parse(s: String): Option[ProcessingCategory] =
    s.split(".").headOption.filter(terms.contains).map(_ => ProcessingCategory(s))

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

}
