package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

case class Provenance(term: String)

object Provenance {

  def parse(s: String): Validated[String, Provenance] =
    Validated.fromOption(
      s.split('.').headOption.filter(terms.contains).map(_ => Provenance(s)),
      "Unknown provenance"
    )

  val terms = List(
    "*",
    "DERIVED",
    "TRANSFERRED",
    "USER",
    "USER.DATA-SUBJECT"
  )

  given Decoder[Provenance] =
    Decoder.decodeString.emap(Provenance.parse(_).toEither)

  given Encoder[Provenance] =
    Encoder[String].contramap(_.term)

}
