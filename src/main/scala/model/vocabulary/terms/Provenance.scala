package io.blindnet.privacy
package model.vocabulary.terms

case class Provenance(term: String)

object Provenance {

  def parse(s: String): Option[Provenance] =
    s.split(".").headOption.filter(terms.contains).map(_ => Provenance(s))

  val terms = List(
    "*",
    "DERIVED",
    "TRANSFERRED",
    "USER"
    // "USER.DATA-SUBJECT"
  )

}
