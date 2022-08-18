package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import doobie.util.Get
import sttp.tapir.Schema
import sttp.tapir.Validator

case class Purpose(term: String)

object Purpose {

  def parse(s: String): Validated[String, Purpose] =
    Validated.fromOption(
      terms.find(_ == s).map(_ => Purpose(s)),
      "Unknown purpose of processing"
    )

  def getSubTerms(dc: Purpose): Set[Purpose] = {
    def getSubTerms0(term: String): List[String] =
      val n = terms.filter(t => t.startsWith(s"$term."))
      if n.length == 0 then List(term) else n.flatMap(t => getSubTerms0(t))

    val res =
      if dc.term == "*" then terms.tail.flatMap(t => getSubTerms0(t)) else getSubTerms0(dc.term)

    res.toSet.map(Purpose(_))
  }

  val terms = List(
    "*",
    "ADVERTISING",
    "COMPLIANCE",
    "EMPLOYMENT",
    "JUSTICE",
    "MARKETING",
    "MEDICAL",
    "PERSONALIZATION",
    "PUBLIC-INTERESTS",
    "RESEARCH",
    "SALE",
    "SECURITY",
    "SERVICES",
    "SERVICES.ADDITIONAL-SERVICES",
    "SERVICES.BASIC-SERVICE",
    "SOCIAL-PROTECTION",
    "TRACKING",
    "VITAL-INTERESTS",
    "OTHER-PURPOSE"
  )

  given Decoder[Purpose] =
    Decoder.decodeString.emap(Purpose.parse(_).toEither)

  given Encoder[Purpose] =
    Encoder[String].contramap(_.term)

  given Schema[Purpose] =
    Schema.string.validate(Validator.enumeration(terms.map(Purpose(_)), x => Option(x.term)))

}
